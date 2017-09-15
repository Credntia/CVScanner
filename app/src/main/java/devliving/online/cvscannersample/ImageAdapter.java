package devliving.online.cvscannersample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import devliving.online.cvscanner.util.Util;

/**
 * Created by user on 10/16/16.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageViewHolder>{
    List<Uri> imageUris = new ArrayList<>();

    /**
     * Called when RecyclerView needs a new {@link RecyclerView.ViewHolder} of the given type to represent
     * an item.
     * <p/>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     * <p/>
     * The new ViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(RecyclerView.ViewHolder, int, List)}. Since it will be re-used to display
     * different items in the data set, it is a good idea to cache references to sub views of
     * the View to avoid unnecessary {@link View#findViewById(int)} calls.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView view = new ImageView(parent.getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);
        view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setPadding(8, 8, 8, 8);
        return new ImageViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link RecyclerView.ViewHolder#itemView} to reflect the item at the given
     * position.
     * <p/>
     * Note that unlike {@link ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only
     * use the <code>position</code> parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an item later
     * on (e.g. in a click listener), use {@link RecyclerView.ViewHolder#getAdapterPosition()} which will
     * have the updated adapter position.
     * <p/>
     * Override {@link #onBindViewHolder(RecyclerView.ViewHolder, int, List)} instead if Adapter can
     * handle efficient partial bind.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        if(position < imageUris.size()){
            Uri imageUri = imageUris.get(position);

            Log.d("ADAPTER", "position: " + position + ", uri: " + imageUri);

            Context context = holder.view.getContext();
            Bitmap image = null;

            try {
                int scale = Util.calculateInSampleSize(context, imageUri, holder.view.getWidth(),
                        holder.view.getHeight(), true);
                image = Util.loadBitmapFromUri(context, scale, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("ADAPTER", "decoded image: " + (image != null));
            holder.view.setImageBitmap(image);
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public void add(Uri imageUri){
        int pos = imageUris.size();
        imageUris.add(imageUri);
        notifyItemInserted(pos);
        Log.d("ADAPTER", "added image");
    }

    public void clear(){
        if(imageUris.size() > 0){
            List<String> paths = new ArrayList(imageUris);
            imageUris.clear();
            notifyDataSetChanged();

            for(String path:paths){
                Utility.deleteFilePermanently(path);
            }
        }

        Log.d("ADAPTER", "cleared all images");
    }
}

class ImageViewHolder extends RecyclerView.ViewHolder{
    ImageView view;
    public ImageViewHolder(View itemView) {
        super(itemView);
        if(itemView instanceof ImageView) {
            this.view = (ImageView) itemView;
        }
    }
}
