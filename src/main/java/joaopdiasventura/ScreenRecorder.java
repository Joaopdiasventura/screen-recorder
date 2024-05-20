package joaopdiasventura;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Objects;
import java.util.Scanner;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;

import javax.sound.sampled.*;

public class ScreenRecorder {

    private final Rectangle screenRect;
    private final Robot robot;
    private final FFmpegFrameRecorder recorder;
    private volatile boolean ended;
    private final AudioFormat audioFormat;
    private final DataLine.Info info;
    private final TargetDataLine line;

    public ScreenRecorder(String videoFileName) throws AWTException, LineUnavailableException {
        this.screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        this.robot = new Robot();

        File videoDirectory = new File("videos");
        if (!videoDirectory.exists()) {
            videoDirectory.mkdirs();
        }

        this.recorder = new FFmpegFrameRecorder(videoDirectory + File.separator + videoFileName, screenRect.width, screenRect.height);
        this.recorder.setFormat("mp4");
        this.recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        this.recorder.setVideoQuality(10);
        this.recorder.setFrameRate(30);
        this.recorder.setVideoBitrate(4000 * 1024);
        this.recorder.setGopSize(60);

        this.audioFormat = new AudioFormat(44100, 16, 2, true, true);
        this.info = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Audio line is not supported");
        }
        this.line = (TargetDataLine) AudioSystem.getLine(info);
        this.line.open(audioFormat);

        this.recorder.setAudioChannels(audioFormat.getChannels());
        this.recorder.setSampleRate((int) audioFormat.getSampleRate());
    }

    private Frame convertImageToFrame(BufferedImage bufferedImage) {
        BufferedImage convertedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        ColorConvertOp op = new ColorConvertOp(null);
        op.filter(bufferedImage, convertedImage);
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.getFrame(convertedImage);
    }

    public void start() throws FrameRecorder.Exception, InterruptedException {
        System.out.println("Starting Record...");
        ended = false;
        recorder.start();
        line.start();
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            System.out.println("Press 'q' to stop the recording");
            while (!ended) {
                String input = sc.nextLine();
                if (Objects.equals(input, "q")) {
                    this.ended = true;
                }
            }
            sc.close();
        }).start();

        new Thread(() -> {
            byte[] audioBuffer = new byte[line.getBufferSize() / 5];
            ShortBuffer shortBuffer = ShortBuffer.allocate(audioBuffer.length / 2);

            while (!ended) {
                int bytesRead = line.read(audioBuffer, 0, audioBuffer.length);
                if (bytesRead > 0) {
                    shortBuffer.clear();
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shortBuffer.array(), 0, bytesRead / 2);
                    try {
                        recorder.recordSamples((int) audioFormat.getSampleRate(), audioFormat.getChannels(), shortBuffer);
                    } catch (FrameRecorder.Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        while (!ended) {
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);
            Frame frame = convertImageToFrame(screenCapture);
            recorder.record(frame);
        }
        recorder.stop();
        recorder.release();
        line.stop();
        line.close();
        System.out.println("Recording Ended");
    }

    public static void main(String[] args) throws AWTException, FrameRecorder.Exception, InterruptedException, LineUnavailableException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter The Name Of The Video: ");
        String name = sc.nextLine();
        ScreenRecorder recorder = new ScreenRecorder(name + ".mp4");
        recorder.start();
        sc.close();
    }
}
