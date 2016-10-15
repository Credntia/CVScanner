package devliving.online.cvscanner.camera;

/**
 * Created by user on 10/15/16.
 */
public interface Detector<T> {
    T processFrame(Frame frame);
    void release();
}
