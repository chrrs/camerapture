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
        public int version = 6;

        /// On by default since v4. Without it a client re-downloads every picture it looks at, every
        /// session — on a server with a few thousand posters that's hundreds of megabytes per player
        /// per login, and it all comes off the server's picture bandwidth. Only ever applies to
        /// multiplayer; see ClientPictureStore#shouldCacheToDisk.
        public boolean cachePictures = true;
        public boolean saveScreenshot = false;
        public boolean simpleCameraHud = false;
        public float zoomMouseSensitivity = 0.5f;

        /// When true, renders picture frames across loaded chunks with screen-space LOD culling.
        public boolean distantPictureRendering = true;
        /// When enabled, placed pictures render a physical backing and side edges.
        /// Disabled by default to preserve the original flat-picture appearance.
        public boolean renderPictureFrameBacking = false;

        public int fullTextureBudgetMiB = 512;
        public int thumbnailTextureBudgetMiB = 64;
        public float fullLodPixels = 32.0f;
        public float minimumRenderPixels = 1.5f;

        public void upgrade() {
            if (this.version < 4) {
                // Adopt the new default. Existing configs were written when it was off and almost
                // certainly never changed it deliberately.
                this.cachePictures = DEFAULT.client.cachePictures;
            }
            if (this.version < 5) {
                this.distantPictureRendering = DEFAULT.client.distantPictureRendering;
                this.fullTextureBudgetMiB = DEFAULT.client.fullTextureBudgetMiB;
                this.thumbnailTextureBudgetMiB = DEFAULT.client.thumbnailTextureBudgetMiB;
                this.fullLodPixels = DEFAULT.client.fullLodPixels;
                this.minimumRenderPixels = DEFAULT.client.minimumRenderPixels;
            }
            if (this.version < 6) {
                this.renderPictureFrameBacking = DEFAULT.client.renderPictureFrameBacking;
            }

            if (this.minimumRenderPixels < 0.5f) {
                this.minimumRenderPixels = DEFAULT.client.minimumRenderPixels;
            }
            if (this.fullLodPixels <= this.minimumRenderPixels) {
                this.fullLodPixels = DEFAULT.client.fullLodPixels;
            }
            if (this.fullTextureBudgetMiB < 32) {
                this.fullTextureBudgetMiB = DEFAULT.client.fullTextureBudgetMiB;
            }
            if (this.thumbnailTextureBudgetMiB < 8) {
                this.thumbnailTextureBudgetMiB = DEFAULT.client.thumbnailTextureBudgetMiB;
            }

            this.version = DEFAULT.client.version;
        }
    }

    /// Server-specific config options.
    public static class Server {
        public int version = 6;

        public int maxImageBytes = 500_000;
        public int maxImageResolution = 1920;
        public int thumbnailResolution = 128;
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
            if (this.version < 6) {
                this.thumbnailResolution = DEFAULT.server.thumbnailResolution;
            }

            if (this.maxImageBytes < 10_000) {
                this.maxImageBytes = DEFAULT.server.maxImageBytes;
            }
            if (this.maxImageResolution < 64) {
                this.maxImageResolution = DEFAULT.server.maxImageResolution;
            }
            if (this.thumbnailResolution < 32 || this.thumbnailResolution > 512) {
                this.thumbnailResolution = DEFAULT.server.thumbnailResolution;
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