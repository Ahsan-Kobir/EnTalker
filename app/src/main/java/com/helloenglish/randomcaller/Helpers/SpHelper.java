package com.helloenglish.randomcaller.Helpers;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Singleton;

@Singleton
public class SpHelper {
    private SharedPreferences sp;
    private static SpHelper instanceOfMe;
    public static synchronized SpHelper getPrefs(Context context){
        if(instanceOfMe==null){
            instanceOfMe = new SpHelper(context);
        }
        return instanceOfMe;
    }
    SpHelper(Context context){
        this.sp = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
    }

    public void setLastCaller(String lastCallerId){
        sp.edit().putString("lastCaller", lastCallerId).commit();
    }

    public String getLastCaller(){
        return sp.getString("lastCaller", null);
    }
}
