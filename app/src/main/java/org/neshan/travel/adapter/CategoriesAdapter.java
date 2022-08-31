package org.neshan.travel.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.neshan.travel.R;
import org.neshan.travel.activity.MapActivity;
import org.neshan.travel.model.Category;

import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoriesViewHolder> {

    private List<Category> categories;
    private Context context;

    @NonNull
    @Override
    public CategoriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_category, parent, false);
        return new CategoriesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriesViewHolder holder, int position) {
        holder.lblName.setText(categories.get(position).getName());
        Picasso.get().load(categories.get(position).getImageAddress()).into(holder.img);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public class CategoriesViewHolder extends RecyclerView.ViewHolder {

        private AppCompatTextView lblName;
        private ImageView img;

        public CategoriesViewHolder(@NonNull View itemView) {
            super(itemView);

            lblName = itemView.findViewById(R.id.lbl_name);
            img = itemView.findViewById(R.id.img_category);

            itemView.setOnClickListener(view -> {
                Intent intent = new Intent(context, MapActivity.class);
                intent.putExtra(MapActivity.CATEGORY_ID, categories.get(getAdapterPosition()).getId());
                context.startActivity(intent);
            });
        }
    }
}
