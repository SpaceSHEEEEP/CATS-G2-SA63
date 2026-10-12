package sg.edu.iss.cats.captcha;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

final class CaptchaArt {
    private CaptchaArt() {}

    private static BufferedImage cat(String name) throws IOException {
        try (InputStream in = CaptchaArt.class.getResourceAsStream("/static/images/cats/" + name + ".png")) {
            if (in == null) throw new IOException("Missing CAPTCHA illustration: " + name);
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new IOException("Invalid CAPTCHA illustration: " + name);
            return img;
        }
    }
    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g;
    }
    private static void drawFit(Graphics2D g, BufferedImage source, int x, int y, int w, int h) {
        double scale = Math.min((double)w / source.getWidth(), (double)h / source.getHeight());
        int dw = (int)(source.getWidth() * scale), dh = (int)(source.getHeight() * scale);
        g.drawImage(source, x + (w-dw)/2, y + (h-dh)/2, dw, dh, null);
    }
    private static byte[] png(BufferedImage image) throws IOException {
        var out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) throw new IOException("PNG encoder unavailable");
        return out.toByteArray();
    }

    static byte[] orderedIconBoard(String catName, String[] symbols, int[] angles,
            int[] sizes, int[] centersX, int[] centersY) throws IOException {
        if (symbols.length != 9 || angles.length != 9 || sizes.length != 9
                || centersX.length != 9 || centersY.length != 9)
            throw new IllegalArgumentException("Nine scattered objects are required");
        int w = 340, h = 255;
        var canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(canvas);
        g.setPaint(new GradientPaint(0,0,new Color(236,245,253),w,h,new Color(255,240,230)));
        g.fillRect(0,0,w,h);
        // The cat is the actual background, not a watermark behind a 3x3 grid.
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.73f));
        drawFit(g, cat(catName), 6, 0, w-12, h);
        g.setComposite(AlphaComposite.SrcOver);
        for (int i=0;i<9;i++) {
            AffineTransform before = g.getTransform();
            g.translate(centersX[i], centersY[i]);
            g.rotate(Math.toRadians(angles[i]));
            // Variable-size symbols are drawn by the server at unpredictable positions.
            drawIcon(g, symbols[i], sizes[i]);
            g.setTransform(before);
        }
        g.dispose(); return png(canvas);
    }

    private static void drawIcon(Graphics2D g, String kind, int size) {
        double r=size/2.0;
        g.setStroke(new BasicStroke(Math.max(2f,size/13f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch (kind) {
            case "star" -> {
                Path2D s=new Path2D.Double();
                for(int i=0;i<10;i++){
                    double a=-Math.PI/2+i*Math.PI/5, len=i%2==0?r:r*0.46;
                    if(i==0)s.moveTo(Math.cos(a)*len,Math.sin(a)*len);
                    else s.lineTo(Math.cos(a)*len,Math.sin(a)*len);
                } s.closePath(); g.setColor(new Color(230,162,27));g.fill(s);g.setColor(new Color(115,83,20));g.draw(s);
            }
            case "fish" -> {
                g.setColor(new Color(52,136,201)); g.fillOval((int)(-r*.7),(int)(-r*.42),(int)(r*1.5),(int)(r*.84));
                Path2D tail=new Path2D.Double(); tail.moveTo(-r*.58,0); tail.lineTo(-r*1.16,-r*.62);
                tail.lineTo(-r*1.16,r*.62);tail.closePath();g.fill(tail);
                g.setColor(Color.WHITE);g.fillOval((int)(r*.42),(int)(-r*.18),5,5);
                g.setColor(Color.BLACK);g.fillOval((int)(r*.48),(int)(-r*.12),2,2);
            }
            case "heart" -> {
                Path2D p=new Path2D.Double();p.moveTo(0,r*.77);
                p.curveTo(-r*1.45,-r*.05,-r*.92,-r*1.15,0,-r*.36);
                p.curveTo(r*.92,-r*1.15,r*1.45,-r*.05,0,r*.77);
                g.setColor(new Color(226,67,102));g.fill(p);g.setColor(new Color(146,36,66));g.draw(p);
            }
            case "moon" -> {
                Area crescent=new Area(new Ellipse2D.Double(-r,-r,2*r,2*r));
                crescent.subtract(new Area(new Ellipse2D.Double(-r*.15,-r*1.13,1.95*r,1.95*r)));
                g.setColor(new Color(99,87,187));g.fill(crescent);
            }
            case "bell" -> {
                g.setColor(new Color(192,142,41));g.fillRoundRect((int)(-r*.62),(int)(-r*.8),(int)(r*1.24),(int)(r*1.4),(int)r,(int)r);
                g.fillOval((int)(-r*.16),(int)(r*.5),(int)(r*.32),(int)(r*.34));
                g.setColor(new Color(92,67,35));g.drawLine((int)-r,(int)(r*.55),(int)r,(int)(r*.55));
            }
            case "balloon" -> {
                g.setColor(new Color(87,172,113));g.fillOval((int)(-r*.62),(int)-r,(int)(r*1.24),(int)(r*1.54));
                g.setColor(new Color(48,96,62));g.drawLine(0,(int)(r*.50),(int)(r*.23),(int)r);
            }
            case "paw" -> {
                g.setColor(new Color(164,105,67));g.fillOval((int)(-r*.6),(int)(-r*.05),(int)(r*1.2),(int)(r*.85));
                g.fillOval((int)(-r*.83),(int)(-r*.9),(int)(r*.42),(int)(r*.50));
                g.fillOval((int)(-r*.31),(int)(-r*1.07),(int)(r*.42),(int)(r*.50));
                g.fillOval((int)(r*.25),(int)(-r*.9),(int)(r*.42),(int)(r*.50));
            }
            case "flower" -> {
                g.setColor(new Color(183,83,164));
                for(int i=0;i<5;i++){
                    double a=-Math.PI/2+i*2*Math.PI/5;
                    g.fillOval((int)(Math.cos(a)*r*.56-r*.34),(int)(Math.sin(a)*r*.56-r*.34),
                            (int)(r*.68),(int)(r*.68));
                }
                g.setColor(new Color(241,199,36));g.fillOval((int)(-r*.27),(int)(-r*.27),(int)(r*.54),(int)(r*.54));
            }
            case "diamond" -> {
                Path2D p=new Path2D.Double();p.moveTo(0,-r);p.lineTo(r*.78,0);
                p.lineTo(0,r);p.lineTo(-r*.78,0);p.closePath();
                g.setColor(new Color(61,155,182));g.fill(p);g.setColor(new Color(25,95,127));g.draw(p);
            }
            default -> throw new IllegalArgumentException("Unknown icon shape");
        }
    }

    static byte[][] rotationBoards(String catName, int angle, int foodSide,
            String[] distractors) throws IOException {
        if (distractors.length != 3) throw new IllegalArgumentException("Three decoys required");
        int w=300, h=300;
        var board=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=graphics(board);
        g.setPaint(new GradientPaint(0,0,new Color(240,248,255),w,h,new Color(246,245,227)));
        g.fillRect(0,0,w,h);
        int[][] positions={{150,30},{270,150},{150,270},{30,150}};
        int decoyIndex = 0;
        for(int i=0;i<4;i++){
            AffineTransform before=g.getTransform();g.translate(positions[i][0],positions[i][1]);
            if(i==foodSide){
                g.setColor(new Color(203,244,204));g.fillOval(-27,-27,54,54);
                drawIcon(g,"fish",41);
            } else {
                // Wrong targets are recognizable, innocuous objects, not red X marks.
                g.setColor(new Color(255,243,224));g.fillOval(-27,-27,54,54);
                drawIcon(g, distractors[decoyIndex++], 37);
            }
            g.setTransform(before);
        }
        g.dispose();
        var catSprite=new BufferedImage(174,174,BufferedImage.TYPE_INT_ARGB);
        Graphics2D sprite=graphics(catSprite);
        sprite.rotate(Math.toRadians(angle),87,87);
        drawFit(sprite,cat(catName),0,0,174,174);
        sprite.dispose();
        return new byte[][] {png(board),png(catSprite)};
    }

    static byte[] matchingCatsBoard(String[] cats, int[] angles, int[] scales,
            int[] backgrounds, boolean[] mirrored) throws IOException {
        if (cats.length != 9 || angles.length != 9 || scales.length != 9
                || backgrounds.length != 9 || mirrored.length != 9)
            throw new IllegalArgumentException("Nine cat panels are required");
        final int width = 340, height = 318;
        Color[] colors = {
            new Color(234, 244, 252), new Color(255, 238, 231),
            new Color(238, 252, 235), new Color(246, 238, 253),
            new Color(255, 249, 220), new Color(235, 247, 247)
        };
        var board = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(board);
        g.setColor(new Color(224, 233, 243));
        g.fillRect(0, 0, width, height);
        for (int i = 0; i < 9; i++) {
            int x = 6 + (i % 3) * 112;
            int y = 6 + (i / 3) * 103;
            g.setColor(colors[Math.floorMod(backgrounds[i], colors.length)]);
            g.fillRoundRect(x, y, 104, 97, 14, 14);
            g.setColor(new Color(132, 151, 171));
            g.setStroke(new BasicStroke(1.3f));
            g.drawRoundRect(x, y, 104, 97, 14, 14);
            // Distinctive cat expressions remain visible despite modest transformations.
            AffineTransform original = g.getTransform();
            g.translate(x + 52, y + 48);
            g.rotate(Math.toRadians(angles[i]));
            if (mirrored[i]) g.scale(-1, 1);
            int box = Math.min(92, 78 * scales[i] / 100);
            drawFit(g, cat(cats[i]), -box / 2, -box / 2, box, box);
            g.setTransform(original);
        }
        g.dispose();
        return png(board);
    }
}
