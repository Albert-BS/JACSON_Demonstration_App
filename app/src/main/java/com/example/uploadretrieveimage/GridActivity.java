package com.example.uploadretrieveimage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
public class GridActivity extends BaseAdapter {
    private ArrayList<DataClass> dataList;
    private Context context;
    LayoutInflater layoutInflater;
    public GridActivity(Context context, ArrayList<DataClass> dataList) {
        this.context = context;
        this.dataList = dataList;
    }
    @Override
    public int getCount() {
        return dataList.size();
    }
    @Override
    public Object getItem(int i) {
        return null;
    }
    @Override
    public long getItemId(int i) {
        return 0;
    }
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (layoutInflater == null){
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if (view == null){
            view = layoutInflater.inflate(R.layout.grid_item, null);
        }
        ImageView gridImage = view.findViewById(R.id.gridImage);
        TextView gridCaption = view.findViewById(R.id.gridCaption);
        ImageView blurImage = view.findViewById(R.id.blurImage);

        blurImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, FullscreenImageActivity.class);
                intent.putExtra("imageURL", dataList.get(i).getImageURL());

                String username = dataList.get(i).getUsername();
                String imageURL = dataList.get(i).getImageURL();
                MyAdapter myAdapter = new MyAdapter(username, imageURL);

                myAdapter.uploadRequest();

                context.startActivity(intent);
            }
        });

        Glide.with(context)
                .load(dataList.get(i).getImageURL())
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Bitmap originalBitmap = ((BitmapDrawable) resource).getBitmap();
                        Bitmap blurredBitmap = blurImage(context, originalBitmap, 25f);
                        gridImage.setImageBitmap(blurredBitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
        gridCaption.setText(dataList.get(i).getCaption());
        return view;
    }

    private Bitmap blurImage(Context context, Bitmap bitmap, float radius) {
        Bitmap blurredBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        RenderScript rsContext = RenderScript.create(context);
        Allocation inputAllocation = Allocation.createFromBitmap(rsContext, bitmap);
        Allocation outputAllocation = Allocation.createFromBitmap(rsContext, blurredBitmap);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext));
        blurScript.setRadius(radius);
        blurScript.setInput(inputAllocation);
        blurScript.forEach(outputAllocation);
        outputAllocation.copyTo(blurredBitmap);

        inputAllocation.destroy();
        outputAllocation.destroy();
        blurScript.destroy();
        rsContext.destroy();

        return blurredBitmap;
    }
}
