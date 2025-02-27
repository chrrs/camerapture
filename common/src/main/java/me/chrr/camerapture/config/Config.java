package me.chrr.camerapture.config;


public class Config {
    public static Config DEFAULT = new Config();

    public Client client = new Client();
    public Server server = new Server();

    public static class Client {
        public int version = 3;

        public boolean cachePictures = false;
        public boolean saveScreenshot = false;
        public boolean simpleCameraHud = false;
        public float zoomMouseSensitivity = 0.5f;

        public void upgrade() {
            this.version = DEFAULT.client.version;
        }
    }

    public static class Server {
        public int version = 4;

        public int maxImageBytes = 500_000;
        public int maxImageResolution = 1920;
        public int msPerPicture = 20;
        public boolean canRotatePictures = true;
        public boolean checkFramePosition = false;
        public boolean allowUploading = true;

        public void upgrade() {
            this.version = DEFAULT.server.version;
        }
    }
}