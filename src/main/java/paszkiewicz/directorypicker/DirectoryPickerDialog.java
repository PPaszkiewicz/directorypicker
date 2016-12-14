package paszkiewicz.directorypicker;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


/**
 * Dialog for picking a specific directory. Can return picked directory or callback for file
 * preview.
 */
public class DirectoryPickerDialog extends DialogFragment {
	public final static String TAG = "paszkiewicz.directorypicker" +
			".DirectoryPickerDialog:TAG";
	private final static String ARG_CURRENTPATH = "ARG_CURRENTPATH";
	private final static String SAVE_SELECTED_FILE = "SAVE_SELECTED_FILE";
	private final static String SAVE_IS_EDITING_NEW_DIRECTORY = "SAVE_IS_EDITING_NEW_DIRECTORY";

	private DirectoryPickedCallback callback;

	private File currentDirectory;
	private File selectedFile;
	private DirectoryContentAdapter directoryContentAdapter;
	private FileDetailsHandler fileDetailsHandler;

	private NewFolderEditTextHandler newFolderEditTextHandler;
	private ImageView buttonUp;
	private ImageView buttonHome;
	private TextView pathTextView;
	private RecyclerView contentRecycler;

	private View emptyContentView;
	private View deleteEmptyView;
	private Button deleteButton;
	private View recreateMissingView;
	private Button recreateButton;

	public static DirectoryPickerDialog newInstance() {
		return newInstance(Environment.getExternalStorageDirectory().getAbsolutePath());
	}

	public static DirectoryPickerDialog newInstance(String currentPath) {
		DirectoryPickerDialog dpd = new DirectoryPickerDialog();
		Bundle b = new Bundle();
		b.putString(ARG_CURRENTPATH, currentPath);
		dpd.setArguments(b);
		return dpd;
	}


	@SuppressWarnings("ConstantConditions")
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			currentDirectory = new File(savedInstanceState.getString(ARG_CURRENTPATH));
		} else {
			currentDirectory = new File(getArguments().getString(ARG_CURRENTPATH));
		}
		@SuppressLint("InflateParams")
		View v = getActivity().getLayoutInflater().inflate(R.layout.directorypicker_dialog_layout,
				null);
		Util.ViewFinder vf = new Util.ViewFinder(v);

		//setup behavior of new folder editing bar
		newFolderEditTextHandler = new NewFolderEditTextHandler(this, vf);
		if (savedInstanceState != null)
			newFolderEditTextHandler.setIsNewFolderEditBarShown(savedInstanceState.getBoolean
					(SAVE_IS_EDITING_NEW_DIRECTORY, false), false);

		//recycler view with content of current directory
		contentRecycler = vf.f(R.id.directorypicker_recycler_dir_content);
		directoryContentAdapter = new DirectoryContentAdapter(this);
		contentRecycler.setAdapter(directoryContentAdapter);
		contentRecycler.setItemAnimator(new DefaultItemAnimator());
		contentRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

		//empty view
		emptyContentView = vf.f(R.id.directorypicker_recyclerview_emptylayout);
		deleteEmptyView = vf.f(R.id.directorypicker_layout_empty_deleteDir);
		deleteButton = vf.f(R.id.directorypicker_dialog_empty_deletedir);
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deleteCurrentDirectory();
			}
		});
		recreateMissingView = vf.f(R.id.directorypicker_layout_empty_createDir);
		recreateButton = vf.f(R.id.directorypicker_dialog_empty_createDir);
		recreateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				recreateFolder();
			}
		});


		//bottomview with selected directory or file
		fileDetailsHandler = new FileDetailsHandler(this, vf.f(R.id
				.directorypicker_include_filedetails));

		//buttons on directory path top bar
		buttonUp = vf.f(R.id.directorypicker_button_up);
		buttonUp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				File parent = currentDirectory.getParentFile();
				if (parent != null)
					changeCurrentDirectory(parent);
				else
					Toast.makeText(getContext(), "Already at home directory", Toast.LENGTH_SHORT)
							.show();
			}
		});

		buttonHome = vf.f(R.id.directorypicker_button_home);
		buttonHome.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				changeCurrentDirectory(Util.getHomeDir(currentDirectory));
			}
		});

		pathTextView = vf.f(R.id.directorypicker_text_dirpath);

		changeCurrentDirectory(currentDirectory);

		//restore selected file
		if (savedInstanceState != null) {
			String selected = savedInstanceState.getString(SAVE_SELECTED_FILE);
			if (selected != null)
				onSelectedFileChanged(new File(selected));
		}

		return new AlertDialog.Builder(getContext())
				.setNegativeButton("cancel", null)
				.setNeutralButton("Default", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						callback.directoryPickedDefault();
					}
				})
				.setPositiveButton("select", new DialogInterface
						.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						callback.directoryPicked(currentDirectory);
					}
				})
				.setView(v)
				.create();
	}

	/**
	 * Change and navigate to a directory
	 *
	 * @param newDirectory newly selected directory
	 * @return true
	 */
	public boolean changeCurrentDirectory(File newDirectory) {
		currentDirectory = newDirectory;
		selectedFile = null;
		fileDetailsHandler.setCurrentFile(newDirectory);
		directoryContentAdapter.setCurrentDirectory(newDirectory);
		contentRecycler.scrollToPosition(0);
		buttonUp.setEnabled(newDirectory.getParentFile() != null);
		buttonHome.setEnabled(!newDirectory.equals(Util.getHomeDir(newDirectory)));
		pathTextView.setText(currentDirectory.getAbsolutePath());
		return true;
	}

	/**
	 * Notify dialog that a file was selected
	 *
	 * @param selectedFile selected file
	 * @return true
	 */
	boolean onSelectedFileChanged(File selectedFile) {
		this.selectedFile = selectedFile;
		if (selectedFile == null) {
			fileDetailsHandler.setCurrentFile(currentDirectory);
		} else
			fileDetailsHandler.setCurrentFile(selectedFile);
		return true;
	}

	/**
	 * Show or hide "empty directory" views
	 *
	 * @param isEmpty true if empty directory is opened, false if it has content
	 */
	void onEmptyDirectoryOpened(boolean isEmpty) {
		contentRecycler.setVisibility(!isEmpty ? View.VISIBLE : View.INVISIBLE);
		emptyContentView.setVisibility(isEmpty ? View.VISIBLE : View.INVISIBLE);
		if (isEmpty) {
			boolean exists = currentDirectory.exists();
			deleteEmptyView.setVisibility(exists ? View.VISIBLE : View.INVISIBLE);
			deleteButton.setEnabled(exists);
			recreateMissingView.setVisibility(!exists ? View.VISIBLE : View.INVISIBLE);
			recreateButton.setEnabled(!exists);
		} else {
			deleteButton.setEnabled(false);
			recreateButton.setEnabled(false);
		}
	}

	/**
	 * Delete current (empty) directory
	 */
	private void deleteCurrentDirectory() {
		File returnDir = currentDirectory.getParentFile();
		if (returnDir == null)
			returnDir = Util.getHomeDir(currentDirectory);
		if (currentDirectory.delete()) {
			Toast.makeText(getContext(), "Folder " + currentDirectory.getName() + " deleted!",
					Toast.LENGTH_SHORT).show();
		} else
			Toast.makeText(getContext(), "Deleting folder " + currentDirectory.getName() + " " +
					"failed!", Toast.LENGTH_SHORT).show();
		changeCurrentDirectory(returnDir);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ARG_CURRENTPATH, currentDirectory.getAbsolutePath());
		if (selectedFile != null)
			outState.putString(SAVE_SELECTED_FILE, selectedFile.getAbsolutePath());
		outState.putBoolean(SAVE_IS_EDITING_NEW_DIRECTORY, newFolderEditTextHandler
				.isNewFolderEditBarShown());
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			callback = (DirectoryPickedCallback) context;
		} catch (ClassCastException cce) {
			throw new ClassCastException("Activity missing DirectoryPickedCallback interface!");
		}
	}

	public File getSelectedFile() {
		return selectedFile;
	}

	/**
	 * Create a new directory in current directory
	 *
	 * @param name name for new folder
	 */
	public boolean createFolder(String name) {
		File newDir = new File(currentDirectory, name);
		if (newDir.mkdir()) {
			Toast.makeText(getContext(), "Folder " + name + " created!", Toast.LENGTH_SHORT)
					.show();
			changeCurrentDirectory(newDir);
			return true;
		}
		Toast.makeText(getContext(), "Failed to create folder " + name + ".", Toast.LENGTH_SHORT)
				.show();
		return false;
	}

	/**
	 * Recreate the folder we're technically in, but doesn't exist
	 *
	 * @return if recreation was successful
	 */
	private boolean recreateFolder() {
		if (currentDirectory.mkdirs()) {
			Toast.makeText(getContext(), "Folder recreated!", Toast.LENGTH_SHORT)
					.show();
			changeCurrentDirectory(currentDirectory);
			return true;
		}
		Toast.makeText(getContext(), "Failed to recreate folder.", Toast.LENGTH_SHORT).show();
		return false;
	}

	/**
	 * Remove focus from textbox
	 *
	 * @param focusText view to clear
	 */
	public void clearFocus(View focusText) {
		InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService
				(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(focusText.getWindowToken(), 0);
		focusText.clearFocus();
	}

	/**
	 * Call callback to open file we selected
	 */
	public void previewFile() {
		callback.previewFile(selectedFile);
	}

	public interface DirectoryPickedCallback {
		/**
		 * Called when directory was picked
		 *
		 * @param directory picked directory
		 */
		void directoryPicked(File directory);

		/**
		 * Called when default button was pressed
		 */
		void directoryPickedDefault();

		/**
		 * Called when user wants to preview file in your app
		 *
		 * @param file file to preview
		 */
		void previewFile(File file);
	}
}
