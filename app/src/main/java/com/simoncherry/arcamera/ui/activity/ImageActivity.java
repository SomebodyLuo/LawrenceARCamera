package com.simoncherry.arcamera.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.simoncherry.arcamera.R;
import com.simoncherry.arcamera.filter.image.ColorFilter;
import com.simoncherry.arcamera.filter.image.ContrastColorFilter;
import com.simoncherry.arcamera.gl.SGLView;

public class ImageActivity extends AppCompatActivity {

    private SGLView mGLView;
    private boolean isHalf = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        mGLView = (SGLView) findViewById(R.id.glView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_img_filter, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_img_deal:
                isHalf = !isHalf;
                if (isHalf) {
                    item.setTitle("处理一半");
                } else {
                    item.setTitle("全部处理");
                }
                mGLView.getRender().refresh();
                break;
            case R.id.menu_img_default:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.NONE));
                break;
            case R.id.menu_img_gray:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.GRAY));
                break;
            case R.id.menu_img_cool:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.COOL));
                break;
            case R.id.menu_img_warn:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.WARM));
                break;
            case R.id.menu_img_blur:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.BLUR));
                break;
            case R.id.menu_img_mag:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.MAGN));
                break;
            case R.id.menu_img_binary:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.BINARY));
                break;
            case R.id.menu_img_negative:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.NEGATIVE));
                break;
            case R.id.menu_img_emboss:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.EMBOSS));
                break;
            case R.id.menu_img_mosaic:
                mGLView.setFilter(new ContrastColorFilter(this, ColorFilter.Filter.MOSAIC));
                break;

        }
        mGLView.getRender().getFilter().setHalf(isHalf);
        mGLView.requestRender();
        return super.onOptionsItemSelected(item);
    }
}
