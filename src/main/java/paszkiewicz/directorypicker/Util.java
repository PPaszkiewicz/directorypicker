package paszkiewicz.directorypicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.View;

import java.io.File;

/**
 * Static class for some functions.
 */
abstract class Util {

	static boolean fileIsPreviewable(File file) {
		String end = getExtension(file.getName());
		return end != null && (extensionIsImage(end) || extensionIsVideo(end));
	}

	static boolean fileIsImage(File file) {
		String end = getExtension(file.getName());
		return end != null && extensionIsImage(end);
	}

	/**
	 * Check if extension is an image
	 *
	 * @param ext lowercase file extension with leading dot
	 * @return true for .jpg .jpeg .png and .gif
	 */
	private static boolean extensionIsImage(@NonNull String ext) {
		return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals("" +
				".gif");
	}

	static boolean fileIsVideo(File file) {
		String end = getExtension(file.getName());
		return end != null && extensionIsVideo(end);
	}

	/**
	 * Check if extension is a video file
	 *
	 * @param ext lowercase file extension with leading dot
	 * @return true for .webm and .mp4
	 */
	private static boolean extensionIsVideo(@NonNull String ext) {
		return ext.equals(".webm") || ext.equals(".mp4");
	}

	/**
	 * get lowercase extension of file path in string
	 *
	 * @param string file path
	 * @return file extension (with leading dot ex. .jpg .png) or null if no dot exists
	 */
	private static String getExtension(String string) {
		if (string.lastIndexOf('.') == -1) return null;
		return string.substring(string.lastIndexOf('.')).toLowerCase();
	}

	static File getHomeDir(File currentDir) {
		File homeDir = new File("/storage");
		if (!homeDir.exists()) {
			homeDir = new File("/mnt");
			if (!homeDir.exists()) {
				homeDir = currentDir.getParentFile();
				int tooDeep = 35;
				while (tooDeep > 0) {
					tooDeep--;
					File newparent = homeDir.getParentFile();
					if (newparent != null)
						homeDir = newparent;
					else
						break;
				}
			}
		}
		return homeDir;
	}

	static int alphaColorFromAttribute(Context context, @AttrRes int attributedColor, float
			alphaMul) {
		TypedValue val = new TypedValue();
		context.getTheme().resolveAttribute(attributedColor, val, true);
		return multiplyAlpha(val.data, alphaMul);
	}

	private static int multiplyAlpha(int color, float alpha) {
		int newAlpha = (int) (Color.alpha(color) * alpha);
		return Color.argb(newAlpha,
				Color.red(color),
				Color.green(color),
				Color.blue(color));
	}

	/**
	 * Create and s tart animator that fades background color.
	 *
	 * @param view                   View to fade background
	 * @param colorFrom              source color
	 * @param colorTo                target color
	 * @param onAnimationEndRunnable runnable to run when animation ends
	 * @return value animator
	 */
	static ValueAnimator createBackgroundColorAnimator(final View view, int colorFrom, int
			colorTo, @Nullable final Runnable onAnimationEndRunnable) {
		view.setBackgroundColor(colorFrom);
		ValueAnimator bgAnim = ObjectAnimator.ofObject(
				view,
				"backgroundColor",
				new ArgbEvaluator(),
				colorFrom,
				colorTo);
		bgAnim.setDuration(250);
		bgAnim.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (onAnimationEndRunnable != null)
					onAnimationEndRunnable.run();
			}
		});
		bgAnim.start();
		return bgAnim;
	}

	static class ViewFinder {
		private final View root;

		ViewFinder(View root) {
			this.root = root;
		}

		@SuppressWarnings("unchecked")
		<T extends View> T f(int viewId) {
			return (T) root.findViewById(viewId);
		}
	}
}
