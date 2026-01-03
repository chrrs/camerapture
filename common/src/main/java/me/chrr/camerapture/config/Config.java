package me.chrr.camerapture.config;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;

public class Config {
    public static Config DEFAULT = new Config();

    public Client client = new Client();
    public Server server = new Server();

    /// Client-specific config options.
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

    /// Server-specific config options.
    public static class Server {
        public int version = 5;

        public int maxImageBytes = 500_000;
        public int maxImageResolution = 1920;
        public int msPerPicture = 20;
        public boolean canRotatePictures = true;
        public boolean checkFramePosition = false;

        public PermissionLevels permissionLevels = new PermissionLevels();

        @DeprecatedConfigOption
        private boolean allowUploading = true;

        public void upgrade() {
            if (this.version < 5) {
                this.permissionLevels.upload = this.allowUploading ? 0 : 4;
            }

            this.version = DEFAULT.server.version;
        }

        /// Permission levels for various actions that can be taken by players.
        public static class PermissionLevels {
            public static Codec<PermissionLevels> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("takePicture").forGetter(p -> p.takePicture),
                    Codec.INT.fieldOf("upload").forGetter(p -> p.upload)
            ).apply(instance, PermissionLevels::new));

            public PermissionLevels() {
            }

            public PermissionLevels(int takePicture, int upload) {
                this.takePicture = takePicture;
                this.upload = upload;
            }

            public int takePicture = 0;
            public int upload = 0;

            public boolean canTakePicture(Player player) {
                return this.takePicture == 0 || player.permissions().hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.byId(this.takePicture)));
            }

            public boolean canUpload(Player player) {
                return this.upload == 0 || player.permissions().hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.byId(this.upload)));
            }

                                          @Override
            public String toString() {
                return "{takePicture=" + takePicture +
                        ", upload=" + upload +
                        '}';
            }
        }
    }
}