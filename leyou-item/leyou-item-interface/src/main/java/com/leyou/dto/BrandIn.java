package com.leyou.dto;

import java.util.List;

public class BrandIn {

    private String name;// 品牌名称
    private String image;// 品牌图片
    private Character letter;
    private List<Long> cids;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Character getLetter() {
        return letter;
    }

    public void setLetter(Character letter) {
        this.letter = letter;
    }

    public List<Long> getCids() {
        return cids;
    }

    public void setCids(List<Long> cids) {
        this.cids = cids;
    }
}
