package com.sahni.rahul.facedetector.showimage;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.sahni.rahul.facedetector.R;
import com.sahni.rahul.facedetector.utils.Constants;


/**
 * A simple {@link Fragment} subclass.
 */
public class ImageFragment extends Fragment {


    public ImageFragment() {
        // Required empty public constructor
    }

    public static ImageFragment newInstance(String imagePath) {

        Bundle args = new Bundle();
        args.putString(Constants.IMAGE_PATH_KEY, imagePath);
        ImageFragment fragment = new ImageFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image, container, false);

        ImageView imageView = view.findViewById(R.id.image_view);

        String imagePath = getArguments().getString(Constants.IMAGE_PATH_KEY);

        Glide.with(imageView)
                .load(imagePath)
                .into(imageView);

        return view;
    }

}
