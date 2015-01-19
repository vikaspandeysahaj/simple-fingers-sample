package com.neurotec.samples.swing;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

public final class ImageThumbnailFileChooser extends JFileChooser {

	// ===========================================================
	// Static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	private static final int ICON_SIZE = 16;
	private static final Image LOADING_IMAGE = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);

	// ===========================================================
	// Private fields
	// ===========================================================

	private Pattern imageFilePattern;
	private Map<File, ImageIcon> imageCache;
	private Image iconImage;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public ImageThumbnailFileChooser() {
		super();
		init();
	}

	public ImageThumbnailFileChooser(String currentDirectoryPath) {
		super(currentDirectoryPath);
		init();
	}

	public ImageThumbnailFileChooser(File currentDirectory) {
		super(currentDirectory);
		init();
	}

	public ImageThumbnailFileChooser(FileSystemView fsv) {
		super(fsv);
		init();
	}

	public ImageThumbnailFileChooser(File currentDirectory, FileSystemView fsv) {
		super(currentDirectory, fsv);
		init();
	}

	public ImageThumbnailFileChooser(String currentDirectoryPath, FileSystemView fsv) {
		super(currentDirectoryPath, fsv);
		init();
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void init() {
		imageFilePattern = Pattern.compile(".+?\\.(png|jpe?g|gif|tiff?)$", Pattern.CASE_INSENSITIVE);
		imageCache = new WeakHashMap<File, ImageIcon>();
		setFileView(new ThumbnailView());
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected JDialog createDialog(Component parent) {
		JDialog dialog = super.createDialog(parent);
		if (iconImage != null) {
			dialog.setIconImage(iconImage);
		}
		return dialog;
	}

	// ===========================================================
	// Public methods
	// ===========================================================

	public void setIcon(Image image) {
		this.iconImage = image;
	}

	// ===========================================================
	// Inner classes
	// ===========================================================

	private class ThumbnailView extends FileView {

		private final ExecutorService executor = Executors.newCachedThreadPool();

		@Override
		public Icon getIcon(File file) {
			if (!imageFilePattern.matcher(file.getName()).matches()) {
				return null;
			}
			synchronized (imageCache) {
				ImageIcon icon = imageCache.get(file);
				if (icon == null) {
					icon = new ImageIcon(LOADING_IMAGE);
					imageCache.put(file, icon);
					executor.submit(new ThumbnailIconLoader(icon, file));
				}
				return icon;
			}
		}

	}

	private class ThumbnailIconLoader implements Runnable {

		private final ImageIcon icon;
		private final File file;

		ThumbnailIconLoader(ImageIcon i, File f) {
			icon = i;
			file = f;
		}

		private BufferedImage getDefaultIconImage(File f) {
			Icon defaultIcon = FileSystemView.getFileSystemView().getSystemIcon(f);
			BufferedImage image = new BufferedImage(defaultIcon.getIconWidth(), defaultIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
			defaultIcon.paintIcon(null, image.getGraphics(), 0, 0);
			return image;
		}

		@Override
		public void run() {
			try {
				BufferedImage img = ImageIO.read(file);
				if (img == null) {
					img = getDefaultIconImage(file);
				}
				int x;
				int y;
				Image scaledImg;
				if (img.getHeight() >= img.getWidth()) {
					scaledImg = img.getScaledInstance(-1, ICON_SIZE, Image.SCALE_SMOOTH);
					x = (int) Math.round((ICON_SIZE - scaledImg.getWidth(null)) / 2.0);
					y = 0;
				} else {
					scaledImg = img.getScaledInstance(ICON_SIZE, -1, Image.SCALE_SMOOTH);
					x = 0;
					y = (int) Math.round((ICON_SIZE - scaledImg.getHeight(null)) / 2.0);
				}

				BufferedImage imgPadding = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = imgPadding.createGraphics();
				g2d.drawImage(scaledImg, x, y, null);
				g2d.dispose();
				icon.setImage(imgPadding);

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						repaint();
					}
				});
			} catch (RuntimeException e) {
				e.printStackTrace();
				throw e;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}