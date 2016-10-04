package server;
 
import java.awt.*;
import javax.swing.*;
import java.awt.geom.Ellipse2D;
import static java.awt.GraphicsDevice.WindowTranslucency.*;
 
public class ShapedWindow extends JWindow {
	public static final int RADIUS = 50;
	private static final long serialVersionUID = -3903638499678467374L;
	
	public ShapedWindow() {
		super();
 
        setBackground(new Color(0, 0, 0, 0));
        setContentPane(new ShapedPane());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        setAlwaysOnTop(true);
        setOpacity(0);
    }
	
	public class ShapedPane extends JPanel {
		private static final long serialVersionUID = -746456962900238457L;

		public ShapedPane() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(2*RADIUS, 2*RADIUS);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.
            Graphics2D g2d = (Graphics2D) g.create();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHints(hints);
            g2d.setColor(Color.RED);
            g2d.fill(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
            g2d.dispose();
        }
    }
	
	public static void checkTransparency() {
        // Determine what the GraphicsDevice can support.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        final boolean isTranslucencySupported =
            gd.isWindowTranslucencySupported(TRANSLUCENT);
 
        //If shaped windows aren't supported, exit.
        if (!gd.isWindowTranslucencySupported(PERPIXEL_TRANSPARENT)) {
            print("Shaped windows are not supported");
            System.exit(0);
        }
 
        //If translucent windows aren't supported,
        //create an opaque window.
        if (!isTranslucencySupported) {
            print("Translucency is not supported, creating an opaque window");
        }
	}
    
	private static void print(String s) {
		System.out.println(s);
	}
}