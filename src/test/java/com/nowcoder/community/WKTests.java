package com.nowcoder.community;

import java.io.IOException;

public class WKTests {
    public static void main(String[] args) {
        String cmd = "d:/wkhtmltopdf/bin/wkhtmltoimage --quality 75  https://www.nowcoder.com d:/data/wk-img/3.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("okkkkkkk!!!!!!!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}