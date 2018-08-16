package com.elkane.heungsootest;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Created by elkan on 2018-08-08.
 */

public class Util {
    Context utilContext;
    static Intent logIntent;
    public static void setLogBroadcast(Context context)
    {

        logIntent = new Intent("textLog");
    }

    public static void logcat(String msg)
    {
        Log.d("aaaaa",msg);
        if(logIntent!=null)
        logIntent.putExtra("textlog",msg);
    }

    public static void errorLogcat(String msg,Exception e)
    {
        Writer writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        logcat( msg +"\n"+writer.toString());
    }

    public static String addTimeDate(Calendar currentDate, int addTimeType, int addTime , String returnStrFormat)
    {
        if(TextUtils.isEmpty(returnStrFormat))
            returnStrFormat="MM-dd HH:mm:ss.SSS";
        Calendar calcCalendar;
        if (currentDate == null)
            calcCalendar = Calendar.getInstance();
        else
            calcCalendar = (Calendar)currentDate.clone();
        if(addTimeType>-1)
            calcCalendar.add(addTimeType, addTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat(returnStrFormat);
        return dateFormat.format(calcCalendar.getTime());
    }


    /**
     * 조흥수 : model객체 데이터 로깅
     *
     * @param titleText 로그 제목
     * @param instance  로깅할 데이터
     */
    public static void heungsooShowDataLog(String titleText, Object instance) {
        showDataLog(titleText, instance, null, 0, false);
    }

    /**
     * instance를 파일에 로깅
     *
     * @param sourceFileName 해당 클래스명
     * @param titleText      타이틀
     * @param instance       로깅할 인스턴스
     */
    public static void heungsooShowDataFileLog(String sourceFileName, String titleText, Object instance) {
        showDataLog(titleText, instance, sourceFileName, 0, true);
    }

    private static void writeDataLog(String sourceFileName, String log, boolean isFileLogging) {
//        if (isFileLogging) {
//            fileHeungsooLog(sourceFileName, log);
//        } else {
        logcat(log);
//        }
    }

    private static boolean isPrivitiveType(Object instance) {
        //편의상 스트링도 프리미티브로 본당..
        return (instance instanceof String
                || instance instanceof Integer || instance instanceof Boolean || instance instanceof Double
                || instance instanceof Long || instance instanceof Byte || instance instanceof Character
                || instance instanceof Short || instance instanceof Float);
    }

    /**
     * instance를 파일 또는 로그캣 창에 로깅
     *
     * @param titleText      타이틀
     * @param instance       로깅할 인스턴스
     * @param sourceFileName 파일로 로깅할경우 해당 클래스명
     * @param level          디폴트는 0
     * @param isFileLogging  파일로깅여부
     */
    public synchronized static void showDataLog(String titleText, Object instance, String sourceFileName, int level, boolean isFileLogging) {
        int subLevel = level + 1;
        String strLog;
        String prefix = "";
        for (int j = 0; j < level; j++)
            prefix += "\t";

        if (subLevel > 15) {
            strLog = "비정상 인덴트 발견 종료함.";
            writeDataLog(sourceFileName, strLog, isFileLogging);
            return;
        }

        if (!TextUtils.isEmpty(titleText)) {
            String upper = prefix;
            for (int i = 0; i < 22 + titleText.length(); i++) {
                upper += "#";
            }
            strLog = prefix + "##########[" + titleText + "]##########";
            writeDataLog(sourceFileName, upper, isFileLogging);
            writeDataLog(sourceFileName, strLog, isFileLogging);
        }

        if (instance == null) {
            strLog = prefix + "---------------인스턴스가 NULL 값입니다 ---------------";
            writeDataLog(sourceFileName, strLog, isFileLogging);
            return;
        }

        //instance 자체가 primitive type이라면..
        if (isPrivitiveType(instance)) {
            strLog = prefix + "타입 : " + instance.getClass().getSimpleName() + ", value : " + instance;
            writeDataLog(sourceFileName, strLog, isFileLogging);
            return;
        } else if (instance instanceof Collection) {
            strLog = prefix + "리스트 타입 , value : " + instance;
            writeDataLog(sourceFileName, strLog, isFileLogging);
            Collection<?> list = (Collection<?>) instance;
            int idx = 0;
            for (Object item : list) {
                if (!isPrivitiveType(item)) {
                    writeDataLog(sourceFileName, prefix + "---------------[index : " + idx + "]-----------------------", isFileLogging);
                    showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                    writeDataLog(sourceFileName, prefix + "--------------------------------------", isFileLogging);
                    idx++;
                } else {
                    showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                }

            }
            return;
        } else if (instance instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>) instance;
            for (String key : map.keySet()) {
                strLog = prefix + "Key : " + key + ", value : " + map.get(key);
                writeDataLog(sourceFileName, strLog, isFileLogging);
            }
            return;
        } else if (instance.getClass().isArray()) {
            strLog = prefix + "배열 타입 , value : " + instance;
            writeDataLog(sourceFileName, strLog, isFileLogging);
            Object[] list = getArray(instance);
            for (int i = 0; i < list.length; i++) {
                Object item = list[i];
                if (!isPrivitiveType(item)) {
                    writeDataLog(sourceFileName, prefix + "---------------[index : " + i + "]-----------------------", isFileLogging);
                    showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                    writeDataLog(sourceFileName, prefix + "--------------------------------------", isFileLogging);
                } else {
                    showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                }
            }
            return;
        }

        strLog = prefix + "↓↓↓↓↓↓↓↓↓↓[" + instance.getClass().getSimpleName() + "]클래스 로깅 시작:  ↓↓↓↓↓↓↓↓↓↓";
        writeDataLog(sourceFileName, strLog, isFileLogging);
        try {
            //public field 로깅
            Field[] fieldList = instance.getClass().getDeclaredFields();
            for (Field field : fieldList) {
                // public field 만
                if (!Modifier.isPublic(field.getModifiers())) {
                    continue;
                }

                //final field는 패스
                if (Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                Object value = field.get(instance);
                //값이 널이면 패스
                if (value == null) {
                    continue;
                }

                //프리미티브 타입이면 바로 표시 - 문자열도 포함
                if (isPrivitiveType(value)) {
                    strLog = prefix + "필드명 : " + field.getName() + ", value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                } else if (value.getClass().getName().contains("com.elkane.heungsootest")) {
                    strLog = prefix + "[" + value.getClass().getSimpleName() + "]필드명 : " + field.getName() + ", value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                    showDataLog(null, value, sourceFileName, subLevel, isFileLogging);
                } else if (value instanceof Collection) {
                    strLog = prefix + "리스트 타입,필드명 : " + field.getName() + ", value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                    Collection<?> list;
                    if (value instanceof Collection) {
                        list = (Collection<?>) value;
                    } else {
                        list = Collections.singletonList(value);
                    }
                    int idx = 0;
                    for (Object item : list) {
                        if (!isPrivitiveType(item)) {
                            writeDataLog(sourceFileName, prefix + "---------------[index : " + idx + "]-----------------------", isFileLogging);
                            showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                            writeDataLog(sourceFileName, prefix + "--------------------------------------", isFileLogging);
                            idx++;
                        } else {
                            showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                        }
                    }
                } else if (value instanceof Map<?, ?>) {
                    Map<String, Object> map = (Map<String, Object>) value;
                    for (String key : map.keySet()) {
                        strLog = prefix + "Key : " + key + ", value : " + map.get(key);
                        writeDataLog(sourceFileName, strLog, isFileLogging);
                    }
                } else if (value.getClass().isArray()) {
                    strLog = prefix + "배열 타입 , value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                    Object[] list = getArray(value);
                    for (int i = 0; i < list.length; i++) {
                        Object item = list[i];
                        if (!isPrivitiveType(item)) {
                            writeDataLog(sourceFileName, prefix + "---------------[index : " + i + "]-----------------------", isFileLogging);
                            showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                            writeDataLog(sourceFileName, prefix + "--------------------------------------", isFileLogging);
                        } else {
                            showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                        }
                    }
                } else {
                    strLog = prefix + "[" + value.getClass().getSimpleName() + "]필드명 : " + field.getName() + ", value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                }
            }

            //property logging...
            Method[] methodArray = instance.getClass().getDeclaredMethods();
            for (Method innerMethod : methodArray) {
                // public method만
                if (!Modifier.isPublic(innerMethod.getModifiers())) {
                    continue;
                }

                //void return은 제외
                if ("void".equals(innerMethod.getReturnType())) {
                    continue;
                }

                // 파라미터가 없는 method 만
                Class<?>[] currentTypeArray = innerMethod.getParameterTypes();
                if (currentTypeArray.length != 0) {
                    continue;
                }

                //현재 Method의 return 값 생성
                Class<?> noparams[] = {};
                Object value = innerMethod.invoke(instance, (Object[]) noparams);

                //값이 널이면 패스
                if (value == null) {
                    continue;
                }

                //프리미티브 타입이면 바로 표시 - 문자열도 포함
                if (isPrivitiveType(value)) {
                    strLog = prefix + "메소드명 : " + innerMethod.getName() + ", value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                } else if (value.getClass().getName().contains("com.briniclemobile.wibeetalk")) {
                    strLog = prefix + "[" + value.getClass().getSimpleName() + "]메소드명 : " + innerMethod.getName() + ", value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                    showDataLog(null, value, sourceFileName, subLevel, isFileLogging);

                } else if (value instanceof Collection || value.getClass().isArray()) {
                    strLog = prefix + "리스트 타입 ,메소드명 : " + innerMethod.getName() + ", value : " + value;

                    writeDataLog(sourceFileName, strLog, isFileLogging);
                    Collection<?> list;
                    if (value instanceof Collection) {
                        list = (Collection<?>) value;
                    } else {
                        list = Collections.singletonList(value);
                    }
                    int idx = 0;
                    for (Object item : list) {
                        if (!isPrivitiveType(item)) {
                            writeDataLog(sourceFileName, prefix + "---------------[index : " + idx + "]-----------------------", isFileLogging);
                            showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                            writeDataLog(sourceFileName, prefix + "--------------------------------------", isFileLogging);
                            idx++;
                        } else {
                            showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                        }
                    }
                } else if (value instanceof Map<?, ?>) {
                    Map<String, Object> map = (Map<String, Object>) value;
                    for (String key : map.keySet()) {
                        strLog = prefix + "Key : " + key + ", value : " + map.get(key);
                        writeDataLog(sourceFileName, strLog, isFileLogging);
                    }
                } else if (value.getClass().isArray()) {
                    strLog = prefix + "배열 타입 , value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                    Object[] list = getArray(value);
                    for (int i = 0; i < list.length; i++) {
                        Object item = list[i];
                        if (!isPrivitiveType(item)) {
                            writeDataLog(sourceFileName, prefix + "---------------[index : " + i + "]-----------------------", isFileLogging);
                            showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                            writeDataLog(sourceFileName, prefix + "--------------------------------------", isFileLogging);
                        } else {
                            showDataLog(null, item, sourceFileName, subLevel, isFileLogging);
                        }
                    }
                } else {
                    strLog = prefix + "[" + value.getClass().getSimpleName() + "]메소드명 : " + innerMethod.getName() + ", value : " + value;
                    writeDataLog(sourceFileName, strLog, isFileLogging);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Writer writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            strLog = prefix + "XXXXXXXXX[" + instance.getClass().getSimpleName() + "]클래스 로깅 중 에러발생 ㅠㅠXXXXXXXX\n";
            strLog += writer.toString();
            writeDataLog(sourceFileName, strLog, isFileLogging);
            return;
        }
        strLog = prefix + "↑↑↑↑↑↑↑↑↑↑[" + instance.getClass().getSimpleName() + "]클래스 로깅 끝↑↑↑↑↑↑↑↑↑↑";
        writeDataLog(sourceFileName, strLog, isFileLogging);
    }

    private static final Class<?>[] ARRAY_PRIMITIVE_TYPES = {
            int[].class, float[].class, double[].class, boolean[].class,
            byte[].class, short[].class, long[].class, char[].class};

    private static Object[] getArray(Object val) {
        Class<?> valKlass = val.getClass();
        Object[] outputArray = null;

        for (Class<?> arrKlass : ARRAY_PRIMITIVE_TYPES) {
            if (valKlass.isAssignableFrom(arrKlass)) {
                int arrlength = Array.getLength(val);
                outputArray = new Object[arrlength];
                for (int i = 0; i < arrlength; ++i) {
                    outputArray[i] = Array.get(val, i);
                }
                break;
            }
        }
        if (outputArray == null) // not primitive type array
        {
            outputArray = (Object[]) val;
        }

        return outputArray;
    }

    public static boolean isInternetOnline(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager)context.getSystemService (Context.CONNECTIVITY_SERVICE);
        return (conMgr.getActiveNetworkInfo() != null
                && conMgr.getActiveNetworkInfo().isAvailable()
                && conMgr.getActiveNetworkInfo().isConnected());
    }

    public static void notification(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel("channel_id", "channel_name", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 100, 200});
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(notificationChannel);
        }

    }

    /**
     * Notification 띄우기
     * @param context
     * @param title
     * @param message
     */
    public static void sendNotification(Context context,String title,String message)
    {
        Intent callIntent = new Intent(context, MainActivity.class);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        callIntent.putExtra("PUSH_MESSAGE", message);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, callIntent, 0);

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = getNotification(context,title,message,pendingIntent);

        int smallIconId = context.getResources().getIdentifier("right_icon","id",android.R.class.getPackage().getName());
        if(smallIconId!=0
                &&android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
                &&android.os.Build.VERSION.SDK_INT< Build.VERSION_CODES.N)
        {
            notification.contentView.setViewVisibility(smallIconId, View.INVISIBLE);
            notification.bigContentView.setViewVisibility(smallIconId,View.INVISIBLE);
        }
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel("default", "heungsooTest", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("heungsooTest");
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(12313, notification);

    }


    private static Notification getNotification(Context context, String title, String message,PendingIntent pendingIntent)
    {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            return getNotificationNew(context,title,message,pendingIntent);
        else
            return getNotificationOld(context,title, message,pendingIntent);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static Notification getNotificationNew(Context context, String title, String message,PendingIntent pendingIntent) {
        Util.logcat("젤리빈 이상 노티피케이션 가져옴");
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ico_delete_asset);
        Notification.Builder builder;
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder = new Notification.Builder(context,"default");
        else
            builder = new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ico_delete_asset);
        builder.setLargeIcon(largeIcon).setTicker(title).setContentTitle(title);
        builder.setContentText(message).setAutoCancel(true);
        builder.setStyle(getBuilderStyle(title, message));
        builder.setContentIntent(pendingIntent);
        builder.setDefaults(Notification.DEFAULT_VIBRATE);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        return builder.build();
    }


    private static Notification getNotificationOld(Context context, String title, String message,PendingIntent pendingIntent) {
        Util.logcat("옛날 버전 노티피케이션 가져옴");
        int icon = R.drawable.ico_delete_asset;
        long currentTime = System.currentTimeMillis();
        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
        {
            notification = new Notification.Builder(context)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(icon)
                    .setContentIntent(pendingIntent)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .getNotification();
        }
        else
        {
            notification = new Notification(icon, title,currentTime);
            try
            {
                Method deprecatedMethod = notification.getClass().getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
                deprecatedMethod.invoke(notification, context, title, null, pendingIntent);
            }
            catch (Exception e)
            {
                Util.logcat("에러발생 흥ㅇㅇㅇ");
            }
            notification.defaults = Notification.DEFAULT_ALL;
        }
//            notif.defaults = Notification.DEFAULT_ALL;
//            notif.flags |= Notification.FLAG_AUTO_CANCEL;


        return notification;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Notification.BigTextStyle getBuilderStyle(String title, String message) {
        Notification.BigTextStyle style = new Notification.BigTextStyle();
        style.setSummaryText("앱 이름");
        style.setBigContentTitle(title);
        style.bigText(message);
        return style;
    }

}
