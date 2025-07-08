package com.plantcare.diseasedetector.ui.info;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.plantcare.diseasedetector.data.models.DiseaseInfo;

/**
 * ViewPager2 adapter for disease information tabs
 */
public class DiseaseInfoPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 3;
    private DiseaseInfo diseaseInfo;

    public DiseaseInfoPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void setDiseaseInfo(DiseaseInfo diseaseInfo) {
        this.diseaseInfo = diseaseInfo;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return SymptomsFragment.newInstance(diseaseInfo);
            case 1:
                return TreatmentFragment.newInstance(diseaseInfo);
            case 2:
                return PreventionFragment.newInstance(diseaseInfo);
            default:
                return SymptomsFragment.newInstance(diseaseInfo);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}

// FILE: app/src/main/java/com/plantcare/diseasedetector/ui/info/DiseaseInfoPagerAdapter.java
// LOCATION: Create this file in the ui/info package