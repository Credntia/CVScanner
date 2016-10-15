package devliving.online.cvscanner.camera;

import java.nio.ByteBuffer;

/**
 * Created by user on 10/15/16.
 */
public class Frame {
    ByteBuffer imageData;
    Size imageSize;
    int frameId;
    long timestamp;
    int rotation;

    public Frame(ByteBuffer imageData, Size imageSize, int frameId, long timestamp, int rotation) {
        this.imageData = imageData;
        this.imageSize = imageSize;
        this.frameId = frameId;
        this.timestamp = timestamp;
        this.rotation = rotation;
    }

    public ByteBuffer getImageData() {
        return imageData;
    }

    public void setImageData(ByteBuffer imageData) {
        this.imageData = imageData;
    }

    public Size getImageSize() {
        return imageSize;
    }

    public void setImageSize(Size imageSize) {
        this.imageSize = imageSize;
    }

    public int getFrameId() {
        return frameId;
    }

    public void setFrameId(int frameId) {
        this.frameId = frameId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }
}
