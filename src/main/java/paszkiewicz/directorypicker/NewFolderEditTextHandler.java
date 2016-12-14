package paszkiewicz.directorypicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Handles displaying edittext and creating new folder.
 */
class NewFolderEditTextHandler implements TextView.OnEditorActionListener, TextWatcher {
	private final DirectoryPickerDialog dialog;
	private final View newFolderBar;
	private final TextInputLayout editTextLayout;
	private final EditText editText;
	private final ImageView acceptButton;
	private final ImageView declineButton;
	private final FloatingActionButton fab;

	private boolean newFolderEditBarShown = false;

	NewFolderEditTextHandler(final DirectoryPickerDialog dialog, Util.ViewFinder vf) {
		this.dialog = dialog;
		newFolderBar = vf.f(R.id.directorypicker_layout_newfolderbar);
		editTextLayout = vf.f(R.id.directorypicker_textinputlayout_createfolder);
		editText = editTextLayout.getEditText();
		acceptButton = vf.f(R.id.directorypicker_button_createfolder_accept);
		declineButton = vf.f(R.id.directorypicker_button_createfolder_cancel);
		fab = vf.f(R.id.directorypicker_button_createfolder_fab);

		acceptButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				createFolder();
			}
		});
		acceptButton.setEnabled(false);

		declineButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setIsNewFolderEditBarShown(false, true);
			}
		});

		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setIsNewFolderEditBarShown(true, true);
			}
		});

		//noinspection ConstantConditions
		editText.setOnEditorActionListener(this);
		editText.addTextChangedListener(this);
	}

	void setIsNewFolderEditBarShown(final boolean shown, boolean animate) {
		newFolderEditBarShown = shown;
		if (!shown) {
			dialog.clearFocus(editText);
			fab.show();
		} else
			fab.hide();
		if (!animate) {
			int translationY = shown ? 0 : getBarHeightPixels();
			//noinspection ResourceType
			newFolderBar.setTranslationY(translationY);
			newFolderBar.setVisibility(shown ? View.VISIBLE : View.GONE);
		} else {
			newFolderBar.setVisibility(View.VISIBLE);
			newFolderBar.animate()
					.translationY(!shown ? getBarHeightPixels() : 0)
					.setDuration(250)
					.setInterpolator(new LinearOutSlowInInterpolator())
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							if (!shown)
								newFolderBar.setVisibility(View.GONE);
						}
					});
		}
	}

	/**
	 * @return true if view is expanded
	 */
	public boolean isNewFolderEditBarShown() {
		return newFolderEditBarShown;
	}

	private void createFolder() {
		if (dialog.createFolder(editText.getText().toString())) {
			dialog.clearFocus(editText);
			editText.setText(null);
			setIsNewFolderEditBarShown(false, true);
		}
	}

	private int getBarHeightPixels() {
		return dialog.getContext().getResources().getDimensionPixelSize(R.dimen
				.directorypicker_newfolder_bar_height);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE) {
			if (v.getText().length() < 1)
				dialog.clearFocus(v);
			else {
				createFolder();
			}
			return true;
		}
		return false;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		acceptButton.setEnabled(s.length() > 0);
	}
}
