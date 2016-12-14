package paszkiewicz.directorypicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.text.format.Formatter;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

/**
 * Displays details of current file.
 */
class FileDetailsHandler {
	private DirectoryPickerDialog dialog;
	private TextView filename;
	private TextView filedetails;
	private ImageView imageView;
	private ImageView openImageButton;

	FileDetailsHandler(final DirectoryPickerDialog dialog, View fileDetailsRoot) {
		this.dialog = dialog;
		Util.ViewFinder vf = new Util.ViewFinder(fileDetailsRoot);
		filename = vf.f(R.id.directorypicker_dialog_details_text_primary);
		filedetails = vf.f(R.id.directorypicker_dialog_details_text_secondary);
		imageView = vf.f(R.id.directorypicker_dialog_details_image);
		openImageButton = vf.f(R.id.directorypicker_dialog_details_button_view);
		openImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.previewFile();
			}
		});
	}

	void setCurrentFile(File file) {
		filename.setText(file.getName());
		boolean showopenImageButton = false;
		if (file.isDirectory()) {
			filedetails.setText(file.getParent());
			imageView.setImageResource(R.drawable.ic_folder_text_primary);
		} else {
			filedetails.setText(Formatter.formatFileSize(filedetails.getContext(), file.length()));
			if (Util.fileIsVideo(file)) {
				imageView.setImageResource(R.drawable.ic_video_text_primary);
				showopenImageButton = true;
			} else if (Util.fileIsImage(file)) {
				imageView.setImageResource(R.drawable.ic_image_text_primary);
				showopenImageButton = true;
			} else
				imageView.setImageResource(R.drawable.ic_file_text_primary);
		}
		boolean buttonStateChanged = openImageButton.isClickable() != showopenImageButton;
		openImageButton.setClickable(showopenImageButton);
		if (buttonStateChanged)
			animateButton(showopenImageButton);
	}

	private void animateButton(final boolean in) {
		final float target = in ? 1.0f : 0.0f;
		if (in)
			openImageButton.setVisibility(View.VISIBLE);
		TimeInterpolator inter;
		if (in) inter = new OvershootInterpolator(2.75f);
		else inter = new AccelerateInterpolator(1.3f);

		openImageButton.animate().setListener(null);
		openImageButton
				.animate()
				.scaleX(target)
				.scaleY(target)
				.setInterpolator(inter)
				.setDuration(150)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						if (!in)
							openImageButton.setVisibility(View.INVISIBLE);
					}
				});
	}
}
