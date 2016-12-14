package paszkiewicz.directorypicker;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Adapter for listing content of directory.
 */
class DirectoryContentAdapter extends RecyclerView.Adapter<DirectoryContentAdapter
		.DirectoryViewHolder> {
	private final static int ITEM_RESOURCE = R.layout.directorypicker_recyclerview_item;
	private File[] fileList;
	private DirectoryPickerDialog dialog;

	private DirectoryViewHolder selectedHolder;
	private int evenBackgroundColor;
	private int selectedBackgroundColor;
	private int transparentSelectedBackgroundColor;

	DirectoryContentAdapter(DirectoryPickerDialog dialog) {
		this.dialog = dialog;
		evenBackgroundColor = Util.alphaColorFromAttribute(dialog.getContext(), R.attr
				.colorPrimary, 0.15f);
		selectedBackgroundColor = Util.alphaColorFromAttribute(dialog.getContext(), R.attr
				.colorAccent, 0.25f);
		transparentSelectedBackgroundColor = Util.alphaColorFromAttribute(dialog.getContext(), R
				.attr.colorAccent, .0f);
	}

	/**
	 * Notify adapter of navigation to another directory.
	 *
	 * @param directory directory to display
	 */
	void setCurrentDirectory(File directory) {
		fileList = createFileList(directory);
		dialog.onEmptyDirectoryOpened(fileList == null || fileList.length == 0);
		selectedHolder = null;
		notifyDataSetChanged();
	}

	@Override
	public DirectoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = dialog.getActivity().getLayoutInflater().inflate(ITEM_RESOURCE, parent, false);
		ImageView image = (ImageView) v.findViewById(R.id.directorypicker_recyclerview_item_image);
		if (viewType >= 100) {
			image.setImageResource(R.drawable.ic_folder_primary_dark);
		} else {
			if (viewType >= 20)
				image.setImageResource(R.drawable.ic_video_thumbnail_accent);
			else if (viewType >= 10)
				image.setImageResource(R.drawable.ic_image_thumbnail_accent);
			else
				image.setImageResource(R.drawable.ic_file_primary_dark);

			View selectedBG = v.findViewById(R.id
					.directorypicker_recyclerview_selected_background);
			selectedBG.setBackgroundColor(selectedBackgroundColor);
		}
		if (viewType % 10 == 1) {
			View evenbg = v.findViewById(R.id.directorypicker_recyclerview_even_background);
			evenbg.setVisibility(View.VISIBLE);
			evenbg.setBackgroundColor(evenBackgroundColor);
		}
		return new DirectoryViewHolder(v);
	}

	@Override
	public int getItemViewType(int position) {
		int even = position % 2;
		if (fileList[position].isDirectory())
			return even + 100;
		if (Util.fileIsImage(fileList[position]))
			return even + 10;
		if (Util.fileIsVideo(fileList[position]))
			return even + 20;
		return even;
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(final DirectoryViewHolder holder, int position) {
		final File file = fileList[position];
		//check if this is the selected file
		if (file.equals(dialog.getSelectedFile())) {
			holder.selectedBG.setVisibility(View.VISIBLE);
			holder.selectedBG.setBackgroundColor(selectedBackgroundColor);
			selectedHolder = holder;
		} else {
			holder.selectedBG.setVisibility(View.INVISIBLE);
		}

		holder.name.setText(file.getName());
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null)
				holder.details.setText(file.listFiles().length + " files");
			else
				holder.details.setText("- -");
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.changeCurrentDirectory(file);
				}
			});
		} else {
			holder.details.setText(Formatter.formatShortFileSize(dialog.getContext(), file.length
					()));
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					setSelected(file, holder);
				}
			});
		}
	}

	@Override
	public int getItemCount() {
		return fileList == null ? 0 : fileList.length;
	}

	/**
	 * set selected background and do callbacks, if called on already selected file clears
	 * selection
	 *
	 * @param file   selected item
	 * @param holder viewholder of item
	 */
	private void setSelected(File file, DirectoryViewHolder holder) {
		if (file.equals(dialog.getSelectedFile())) {
			dialog.onSelectedFileChanged(null);
			selectedHolder = null;
			holder.selectedBG.setVisibility(View.INVISIBLE);
			return;
		}
		if (selectedHolder != null)
			animateSelectedBackground(selectedHolder, false);
		dialog.onSelectedFileChanged(file);
		selectedHolder = holder;
		animateSelectedBackground(holder, true);
	}

	/**
	 * fade in selected background
	 *
	 * @param holder holder
	 * @param in     true to fade in, false to out
	 */
	private void animateSelectedBackground(final DirectoryViewHolder holder, final boolean in) {
		int colorFrom = in ? transparentSelectedBackgroundColor : selectedBackgroundColor;
		int colorTo = in ? selectedBackgroundColor : transparentSelectedBackgroundColor;

		if (holder.backgroundAnim != null)
			holder.backgroundAnim.end();

		if (in) {
			holder.selectedBG.setVisibility(View.VISIBLE);
		}
		holder.backgroundAnim = Util.createBackgroundColorAnimator(holder.selectedBG, colorFrom,
				colorTo, new Runnable() {
					@Override
					public void run() {
						holder.backgroundAnim = null;
						if (!in)
							holder.selectedBG.setVisibility(View.INVISIBLE);
					}
				});
	}

	@Override
	public void onViewDetachedFromWindow(DirectoryViewHolder holder) {
		if (holder.backgroundAnim != null)
			holder.backgroundAnim.end();
		super.onViewDetachedFromWindow(holder);
	}

	/**
	 * Sorts the filelist
	 *
	 * @param file dir to be sorted
	 * @return folders first, then images, then others alphabetically
	 */
	private File[] createFileList(File file) {
		File[] files = file.listFiles();
		if (files == null)
			return null;
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File lhs, File rhs) {
				int l = lhs.isDirectory() ? 1 : 0;
				int r = rhs.isDirectory() ? 1 : 0;
				if (l != r)
					return r - l;

				l = Util.fileIsPreviewable(lhs) ? 1 : 0;
				r = Util.fileIsPreviewable(rhs) ? 1 : 0;
				if (l != r)
					return r - l;

				return lhs.getName().compareToIgnoreCase(rhs.getName());
			}
		});
		return files;
	}

	static class DirectoryViewHolder extends RecyclerView.ViewHolder {
		TextView name;
		TextView details;
		View selectedBG;

		Animator backgroundAnim;

		public DirectoryViewHolder(View itemView) {
			super(itemView);
			Util.ViewFinder f = new Util.ViewFinder(itemView);
			name = f.f(R.id.directorypicker_recyclerview_item_text_primary);
			details = f.f(R.id.directorypicker_recyclerview_item_text_secondary);
			selectedBG = f.f(R.id.directorypicker_recyclerview_selected_background);
		}
	}
}
