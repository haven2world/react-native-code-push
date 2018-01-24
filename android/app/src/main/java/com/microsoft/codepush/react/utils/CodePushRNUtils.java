package com.microsoft.codepush.react.utils;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import com.microsoft.codepush.react.CodePushConstants;
import com.microsoft.codepush.react.exceptions.CodePushMalformedDataException;
import com.microsoft.codepush.react.exceptions.CodePushUnknownException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;

public class CodePushRNUtils extends CodePushUtils {

    public static void log(String message) {
        Log.d(CodePushConstants.REACT_NATIVE_LOG_TAG, "[CodePush] " + message);
    }

    public static WritableArray convertJsonArrayToWritable(JSONArray jsonArr) {
        WritableArray arr = Arguments.createArray();
        for (int i=0; i<jsonArr.length(); i++) {
            Object obj = null;
            try {
                obj = jsonArr.get(i);
            } catch (JSONException jsonException) {
                // Should not happen.
                throw new CodePushUnknownException(i + " should be within bounds of array " + jsonArr.toString(), jsonException);
            }

            if (obj instanceof JSONObject)
                arr.pushMap(convertJsonObjectToWritable((JSONObject) obj));
            else if (obj instanceof JSONArray)
                arr.pushArray(convertJsonArrayToWritable((JSONArray) obj));
            else if (obj instanceof String)
                arr.pushString((String) obj);
            else if (obj instanceof Double)
                arr.pushDouble((Double) obj);
            else if (obj instanceof Integer)
                arr.pushInt((Integer) obj);
            else if (obj instanceof Boolean)
                arr.pushBoolean((Boolean) obj);
            else if (obj == null)
                arr.pushNull();
            else
                throw new CodePushUnknownException("Unrecognized object: " + obj);
        }

        return arr;
    }

    public static WritableMap convertJsonObjectToWritable(JSONObject jsonObj) {
        WritableMap map = Arguments.createMap();
        Iterator<String> it = jsonObj.keys();
        while(it.hasNext()){
            String key = it.next();
            Object obj = null;
            try {
                obj = jsonObj.get(key);
            } catch (JSONException jsonException) {
                // Should not happen.
                throw new CodePushUnknownException("Key " + key + " should exist in " + jsonObj.toString() + ".", jsonException);
            }

            if (obj instanceof JSONObject)
                map.putMap(key, convertJsonObjectToWritable((JSONObject) obj));
            else if (obj instanceof JSONArray)
                map.putArray(key, convertJsonArrayToWritable((JSONArray) obj));
            else if (obj instanceof String)
                map.putString(key, (String) obj);
            else if (obj instanceof Double)
                map.putDouble(key, (Double) obj);
            else if (obj instanceof Integer)
                map.putInt(key, (Integer) obj);
            else if (obj instanceof Boolean)
                map.putBoolean(key, (Boolean) obj);
            else if (obj == null)
                map.putNull(key);
            else
                throw new CodePushUnknownException("Unrecognized object: " + obj);
        }

        return map;
    }

    public static JSONArray convertReadableToJsonArray(ReadableArray arr) {
        JSONArray jsonArr = new JSONArray();
        for (int i=0; i<arr.size(); i++) {
            ReadableType type = arr.getType(i);
            switch (type) {
                case Map:
                    jsonArr.put(convertReadableToJsonObject(arr.getMap(i)));
                    break;
                case Array:
                    jsonArr.put(convertReadableToJsonArray(arr.getArray(i)));
                    break;
                case String:
                    jsonArr.put(arr.getString(i));
                    break;
                case Number:
                    Double number = arr.getDouble(i);
                    if ((number == Math.floor(number)) && !Double.isInfinite(number)) {
                        // This is a whole number.
                        jsonArr.put(number.longValue());
                    } else {
                        try {
                            jsonArr.put(number.doubleValue());
                        } catch (JSONException jsonException) {
                            throw new CodePushUnknownException("Unable to put value " + arr.getDouble(i) + " in JSONArray");
                        }
                    }
                    break;
                case Boolean:
                    jsonArr.put(arr.getBoolean(i));
                    break;
                case Null:
                    jsonArr.put(null);
                    break;
            }
        }

        return jsonArr;
    }

    public static <T> T convertReadableToObject(ReadableMap map, Class<T> classOfT) {
        return CodePushUtils.convertJsonObjectToObject(convertReadableToJsonObject(map), classOfT);
    }

    public static JSONObject convertReadableToJsonObject(ReadableMap map) {
        JSONObject jsonObj = new JSONObject();
        ReadableMapKeySetIterator it = map.keySetIterator();
        while (it.hasNextKey()) {
            String key = it.nextKey();
            ReadableType type = map.getType(key);
            try {
                switch (type) {
                    case Map:
                        jsonObj.put(key, convertReadableToJsonObject(map.getMap(key)));
                        break;
                    case Array:
                        jsonObj.put(key, convertReadableToJsonArray(map.getArray(key)));
                        break;
                    case String:
                        jsonObj.put(key, map.getString(key));
                        break;
                    case Number:
                        jsonObj.put(key, map.getDouble(key));
                        break;
                    case Boolean:
                        jsonObj.put(key, map.getBoolean(key));
                        break;
                    case Null:
                        jsonObj.put(key, null);
                        break;
                    default:
                        throw new CodePushUnknownException("Unrecognized type: " + type + " of key: " + key);
                }
            } catch (JSONException jsonException) {
                throw new CodePushUnknownException("Error setting key: " + key + " in JSONObject", jsonException);
            }
        }

        return jsonObj;
    }

    public static void logBundleUrl(String path) {
        //TODO use Appcenter.log
        log("Loading JS bundle from \"" + path + "\"");
    }

    public static String tryGetString(ReadableMap map, String key) {
        try {
            return map.getString(key);
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    public static <T> T convertWritableMapToObject(WritableMap writableMap, Class<T> classOfT) {
        try {
            JSONObject jsonObject = new JSONObject(writableMap.toString()).optJSONObject("NativeMap");
            return CodePushUtils.convertJsonObjectToObject(jsonObject, classOfT);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new CodePushMalformedDataException(e.toString(), e);
        }
    }

    public static WritableMap convertObjectToWritableMap(Object object) {
        return convertJsonObjectToWritable(CodePushUtils.convertObjectToJsonObject(object));
    }

    public static String findJSBundleInUpdateContents(String folderPath, String expectedFileName) {
        File folder = new File(folderPath);
        File[] folderFiles = folder.listFiles();
        for (File file : folderFiles) {
            String fullFilePath = FileUtils.appendPathComponent(folderPath, file.getName());
            if (file.isDirectory()) {
                String mainBundlePathInSubFolder = findJSBundleInUpdateContents(fullFilePath, expectedFileName);
                if (mainBundlePathInSubFolder != null) {
                    return FileUtils.appendPathComponent(file.getName(), mainBundlePathInSubFolder);
                }
            } else {
                String fileName = file.getName();
                if (fileName.equals(expectedFileName)) {
                    return fileName;
                }
            }
        }

        return null;
    }
}
