package com.app.ectrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<Map<String, Object>> employees = new ArrayList<>();

    public void setEmployees(List<Map<String, Object>> employees) {
        this.employees = employees;
        notifyDataSetChanged();
    }

    private OnEmployeeDeleteListener deleteListener;

    public interface OnEmployeeDeleteListener {
        void onDelete(Map<String, Object> employee);
    }

    public void setOnEmployeeDeleteListener(OnEmployeeDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Map<String, Object> employee = employees.get(position);
        String email = (String) employee.get("email");
        String role = (String) employee.get("userType"); // Assuming userType holds role like 'pharmacy_owner' or



        if (role == null) {
            role = (String) employee.get("role");
        }

        holder.textName.setText(email != null ? email : "Bilinmeyen Kullanıcı");

        String displayRole = "Çalışan";
        if ("pharmacy_owner".equals(role)) {
            displayRole = "Eczane Sahibi";
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
        }
        holder.textRole.setText(displayRole);

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(employee);
            }
        });
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textRole;
        View btnDelete;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textRole = itemView.findViewById(R.id.textRole);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

