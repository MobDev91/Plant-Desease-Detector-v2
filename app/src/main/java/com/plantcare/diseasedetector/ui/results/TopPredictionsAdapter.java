package com.plantcare.diseasedetector.ui.results;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.plantcare.diseasedetector.R;

/**
 * Adapter for displaying top AI predictions in ResultsActivity
 */
public class TopPredictionsAdapter extends RecyclerView.Adapter<TopPredictionsAdapter.PredictionViewHolder> {

    private Context context;
    private ResultsActivity.TopPrediction[] predictions;

    public TopPredictionsAdapter(Context context) {
        this.context = context;
        this.predictions = new ResultsActivity.TopPrediction[0];
    }

    /**
     * Update predictions data
     */
    public void updatePredictions(ResultsActivity.TopPrediction[] predictions) {
        this.predictions = predictions != null ? predictions : new ResultsActivity.TopPrediction[0];
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PredictionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_prediction, parent, false);
        return new PredictionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PredictionViewHolder holder, int position) {
        ResultsActivity.TopPrediction prediction = predictions[position];
        holder.bind(prediction, position + 1);
    }

    @Override
    public int getItemCount() {
        return predictions.length;
    }

    /**
     * ViewHolder class for prediction items
     */
    class PredictionViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvRank;
        private TextView tvClassName;
        private TextView tvConfidence;
        private ProgressBar progressConfidence;

        PredictionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvClassName = itemView.findViewById(R.id.tv_class_name);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
            progressConfidence = itemView.findViewById(R.id.progress_confidence);
        }

        void bind(ResultsActivity.TopPrediction prediction, int rank) {
            // Set rank
            tvRank.setText(String.valueOf(rank));

            // Set class name (formatted for display)
            tvClassName.setText(prediction.getFormattedClassName());

            // Set confidence percentage
            tvConfidence.setText(prediction.getConfidencePercentage());

            // Set progress bar
            int confidencePercent = Math.round(prediction.confidence * 100);
            progressConfidence.setProgress(confidencePercent);

            // Set rank indicator color based on position
            int rankColor;
            int progressColor;
            switch (rank) {
                case 1:
                    rankColor = R.color.healthy_green;
                    progressColor = R.color.healthy_green;
                    break;
                case 2:
                    rankColor = R.color.warning_orange;
                    progressColor = R.color.warning_orange;
                    break;
                default:
                    rankColor = R.color.gray_medium;
                    progressColor = R.color.gray_medium;
                    break;
            }

            // Apply colors
            tvRank.setBackgroundTintList(ContextCompat.getColorStateList(context, rankColor));
            progressConfidence.setProgressTintList(ContextCompat.getColorStateList(context, progressColor));

            // Set text color for rank
            tvRank.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        }
    }
}
