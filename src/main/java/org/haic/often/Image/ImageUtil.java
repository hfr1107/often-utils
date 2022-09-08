package org.haic.often.Image;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;

/**
 * 图片工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/9/8 21:35
 */
public class ImageUtil {
	protected BufferedImage src;

	protected ImageUtil() {
	}

	/**
	 * 创建图片的工具对象
	 *
	 * @param src 图片路径
	 * @return new ImageUtil
	 */
	public static ImageUtil origin(@NotNull String src) {
		return origin(new File(src));
	}

	/**
	 * 创建图片的工具对象
	 *
	 * @param file 图片文件对象
	 * @return new ImageUtil
	 */
	public static ImageUtil origin(@NotNull File file) {
		BufferedImage image;
		try {
			image = ImageIO.read(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return origin(image);
	}

	/**
	 * 创建图片的工具对象
	 *
	 * @param image 图片文件对象
	 * @return new ImageUtil
	 */
	public static ImageUtil origin(@NotNull BufferedImage image) {
		return new ImageUtil().image(image);
	}

	/**
	 * 将图片对象写入文件
	 *
	 * @param image 图片对象
	 * @param src   文件路径
	 * @return 写入状态
	 */
	public static boolean write(@NotNull BufferedImage image, @NotNull String src) {
		return write(image, new File(src));
	}

	/**
	 * 将图片对象写入文件
	 *
	 * @param image 图片对象
	 * @param file  文件对象
	 * @return 写入状态
	 */
	public static boolean write(@NotNull BufferedImage image, @NotNull File file) {
		try {
			String fileName = file.getName();
			ImageIO.write(image, fileName.substring(fileName.lastIndexOf('.') + 1), file);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private ImageUtil image(@NotNull BufferedImage src) {
		this.src = src;
		return this;
	}

	/**
	 * 转灰度图像
	 *
	 * @return 图片对象
	 */
	public BufferedImage toGray() {
		if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
			return src;
		} else { // 图像转灰
			BufferedImage grayImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(src, grayImage);
			return grayImage;
		}
	}

	/**
	 * 放大图像至指定倍数
	 *
	 * @param multiple 放大倍数
	 * @return 图片对象
	 */
	public BufferedImage enlarge(int multiple) {
		return resize(src.getWidth() * multiple, src.getHeight() * multiple);
	}

	/**
	 * 缩小图像至指定倍数
	 *
	 * @param multiple 缩小倍数
	 * @return 图片对象
	 */
	public BufferedImage reduce(int multiple) {
		return resize(src.getWidth() / multiple, src.getHeight() / multiple);
	}

	/**
	 * 缩放图像到指定尺寸
	 *
	 * @param width  宽
	 * @param height 高
	 * @return 图片对象
	 */
	public BufferedImage resize(int width, int height) {
		BufferedImage result = new BufferedImage(width, height, src.getType());
		Graphics graphics = result.getGraphics();
		graphics.drawImage(src.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH), 0, 0, null);
		graphics.dispose();
		return result;
	}

}