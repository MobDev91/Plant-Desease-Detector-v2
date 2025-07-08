// FILE 1: SymptomsFragment.java
package com.plantcare.diseasedetector.ui.info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.data.models.DiseaseInfo;

import java.io.Serializable;

public class SymptomsFragment extends Fragment {
    private static final String ARG_DISEASE_INFO = "disease_info";
    private DiseaseInfo diseaseInfo;

    public static SymptomsFragment newInstance(DiseaseInfo diseaseInfo) {
        SymptomsFragment fragment = new SymptomsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DISEASE_INFO, (Serializable) diseaseInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            diseaseInfo = (DiseaseInfo) getArguments().getSerializable(ARG_DISEASE_INFO);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_symptoms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize UI with disease info
        if (diseaseInfo != null) {
            // Setup symptoms data
        }
    }
}