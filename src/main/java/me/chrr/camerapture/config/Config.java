package me.chrr.camerapture.config;

import com.google.gson.annotations.SerializedName;

public class Config {
    public Client client = new Client();
    public static class Client {
        public static int VERSION = 1;

        @SerializedName("version")
        public int version = VERSION;

        @SerializedName("cache_pictures")
        public boolean cachePictures = true;

        public void upgrade() {
            this.version = VERSION;
        }
    }
}
