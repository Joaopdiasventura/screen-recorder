package org.example;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Scanner;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;

public class ScreenRecorder {

    private final Rectangle screenRect;
    private final Robot robot;
    private final FFmpegFrameRecorder recorder;

    public ScreenRecorder(String videoFileName) throws AWTException {
        this.screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        this.robot = new Robot();
        this.recorder = new FFmpegFrameRecorder(videoFileName, screenRect.width, screenRect.height);
        this.recorder.setFormat("mp4");
        this.recorder.setVideoCodec(27);
        this.recorder.setVideoQuality(1);
        this.recorder.setFrameRate(30);
    }

    public void start() throws FrameRecorder.Exception, InterruptedException {
        System.out.println("Starting Record...");
        recorder.start();
        long startTime = System.currentTimeMillis();
        while (true) {
            long now = System.currentTimeMillis();
            if (now - startTime > 5000) {
                break;
            }
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);
            Frame frame = convertImageToFrame(screenCapture);
            recorder.record(frame,1);
            Thread.sleep(33);
        }
        recorder.stop();
        recorder.release();
        System.out.println("Recording Ended");
    }

    private Frame convertImageToFrame(BufferedImage bufferedImage) {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.getFrame(bufferedImage);
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
