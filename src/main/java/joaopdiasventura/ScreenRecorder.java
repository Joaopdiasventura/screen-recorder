package joaopdiasventura;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.util.Objects;
import java.util.Scanner;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;

public class ScreenRecorder {

    private final Rectangle screenRect;
    private final Robot robot;
    private final FFmpegFrameRecorder recorder;
    private volatile boolean ended;

    public ScreenRecorder(String videoFileName) throws AWTException {
        this.screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        this.robot = new Robot();

        File videoDirectory = new File("videos");
        if (!videoDirectory.exists()) {
            videoDirectory.mkdirs();
        }

        this.recorder = new FFmpegFrameRecorder(videoDirectory + File.separator + videoFileName, screenRect.width, screenRect.height);
        this.recorder.setFormat("mp4");
        this.recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        this.recorder.setVideoQuality(0);
        this.recorder.setFrameRate(30);
        this.recorder.setVideoBitrate(4000 * 1024);
        this.recorder.setGopSize(60);
    }

    public void start() throws FrameRecorder.Exception, InterruptedException {
        System.out.println("Starting Record...");
        ended = false;
        recorder.start();
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            System.out.println("Press 'q' to stop the recording");
            while (!ended) {
                String input = sc.nextLine();
                if (Objects.equals(input, "q")) {
                    stop();
                }
            }
            sc.close();
        }).start();
        while (!ended) {
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);
            Frame frame = convertImageToFrame(screenCapture);
            recorder.record(frame);
            Thread.sleep(60);
        }
        recorder.stop();
        recorder.release();
        System.out.println("Recording Ended");
    }

    public void stop() {
        this.ended = true;
    }

    private Frame convertImageToFrame(BufferedImage bufferedImage) {
        BufferedImage convertedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        ColorConvertOp op = new ColorConvertOp(null);
        op.filter(bufferedImage, convertedImage);
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.getFrame(convertedImage);
    }

    public static void main(String[] args) throws AWTException, FrameRecorder.Exception, InterruptedException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter The Name Of The Video: ");
        String name = sc.nextLine();
        ScreenRecorder recorder = new ScreenRecorder(name + ".mp4");
        recorder.start();
        sc.close();
    }
}
