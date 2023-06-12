package com.example.uploadretrieveimage;

public class DataClass {
    private String imageURL, caption, category, policyId;

    public DataClass(){

    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public DataClass(String imageURL, String caption, String category, String policyId) {
        this.imageURL = imageURL;
        this.caption = caption;
        this.category = category;
        this.policyId = policyId;
    }
}
