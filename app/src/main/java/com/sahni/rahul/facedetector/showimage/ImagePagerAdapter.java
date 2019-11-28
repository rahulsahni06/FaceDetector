package com.sahni.rahul.facedetector.showimage;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.sahni.rahul.facedetector.showimage.ImageFragment;

import java.util.ArrayList;

public class ImagePagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<String> imageArrayList;

    public ImagePagerAdapter(@NonNull FragmentManager fm, int behavior, ArrayList<String> imageArrayList) {
        super(fm, behavior);
        this.imageArrayList = imageArrayList;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return ImageFragment.newInstance(imageArrayList.get(position));
    }

    @Override
    public int getCount() {
        return imageArrayList.size();
    }
}
