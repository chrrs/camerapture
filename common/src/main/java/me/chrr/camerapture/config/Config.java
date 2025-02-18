package me.chrr.camerapture.config;

import com.google.gson.annotations.SerializedName;

public class Config {
    public Client client = new Client();
    public Server server = new Server();

    public static class Client {
        public static int VERSION = 2;

        @SerializedName("version")
        public int version = VERSION;

        @SerializedName("cache_pictures")
        public boolean cachePictures = false;

        @SerializedName("save_screenshot")
        public boolean saveScreenshot = false;

        public void upgrade() {
            this.version = VERSION;
        }
    }

    public static class Server {
        public static int VERSION = 4;

        @SerializedName("version")
        public int version = VERSION;

        @SerializedName("max_image_bytes")
        public int maxImageBytes = 500_000;

        @SerializedName("max_image_resolution")
        public int maxImageResolution = 1920;

        @SerializedName("ms_per_picture")
        public int msPerPicture = 20;

        @SerializedName("can_rotate_pictures")
        public boolean canRotatePictures = true;

        @SerializedName("check_frame_position")
        public boolean checkFramePosition = false;

        @SerializedName("allow_uploading")
        public boolean allowUploading = true;

        public void upgrade() {
            this.version = VERSION;
        }
    }
}