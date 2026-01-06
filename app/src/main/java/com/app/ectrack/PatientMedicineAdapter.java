package com.app.ectrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatientMedicineAdapter extends RecyclerView.Adapter<PatientMedicineAdapter.ViewHolder> {

    private List<PatientMedicine> medicines = new ArrayList<>();
    private int expandedPosition = -1;

    public void setMedicines(List<PatientMedicine> medicines) {
        this.medicines = medicines;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_medicine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PatientMedicine medicine = medicines.get(position);

        holder.textName.setText(medicine.getName());

        if (medicine.getPrescribedDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.textDate.setText("Yazılma: " + sdf.format(medicine.getPrescribedDate().toDate()));
        } else if (medicine.getStartDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.textDate.setText("Yazılma: " + sdf.format(medicine.getStartDate().toDate()));
        } else {
            holder.textDate.setText("Yazılma tarihi bilinmiyor");
        }

        String dosage = medicine.getDosage();
        String duration = medicine.getDuration();
        String instructions = medicine.getUsageInstructions();

        holder.textDosage.setText("Doz: " + (dosage != null ? dosage : "-"));
        holder.textDuration.setText("Süre: " + (duration != null ? duration : "-"));
        holder.textInstructions.setText("Kullanım: " + (instructions != null ? instructions : "-"));

        boolean isExpanded = position == expandedPosition;
        holder.layoutDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.textExpandHint.setText(isExpanded ? "Detayları gizle ▲" : "Detaylar için tıklayın ▼");

        holder.itemView.setOnClickListener(v -> {
            int prevExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : position;

            notifyItemChanged(prevExpanded);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textDate, textDosage, textDuration, textInstructions, textExpandHint;
        LinearLayout layoutDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textDate = itemView.findViewById(R.id.textDate);
            textDosage = itemView.findViewById(R.id.textDosage);
            textDuration = itemView.findViewById(R.id.textDuration);
            textInstructions = itemView.findViewById(R.id.textInstructions);
            textExpandHint = itemView.findViewById(R.id.textExpandHint);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
        }
    }
}

