package me.chrr.camerapture.config;

import com.google.gson.annotations.SerializedName;

public class Config {
    public Client client = new Client();
    public static class Client {
        public static int VERSION = 1;

        @SerializedName("version")
        public int version = VERSION;

        @SerializedName("cache_pictures")
        public boolean cachePictures = false;

        public void upgrade() {
            this.version = VERSION;
        }
    }

    public Server server = new Server();
    public static class Server {
        public static int VERSION = 2;

        @SerializedName("version")
        public int version = VERSION;

        @SerializedName("max_image_bytes")
        public int maxImageBytes = 200_000;

        @SerializedName("max_image_resolution")
        public int maxImageResolution = 1280;

        @SerializedName("ms_per_picture")
        public int msPerPicture = 50;

        public void upgrade() {
            this.version = VERSION;
        }
    }
}
