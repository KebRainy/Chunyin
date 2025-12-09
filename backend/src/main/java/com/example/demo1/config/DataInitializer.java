package com.example.demo1.config;

import com.example.demo1.service.WikiService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final WikiService wikiService;

    @Override
    public void run(String... args) {
        wikiService.ensureDefaultPage(
            "craft-beer-basics",
            "精酿啤酒入门",
            "从麦芽、酒花到品鉴流程，帮你快速掌握精酿啤酒的基础知识。",
            """
            ## 经典风格
            - 认识 IPA、Stout 等经典风格的香气、色泽与适合搭配
            - 通过原料与酿造工艺判断风味层次与酒体

            ## 品鉴要点
            1. 观察酒体颜色、泡沫与挂杯情况
            2. 分阶段记录香气、入口与回味，形成自己的风味笔记
            """
        );

        wikiService.ensureDefaultPage(
            "whisky-intro",
            "威士忌指南",
            "梳理不同产区、熟成方式与调饮方法，下次碰到任何一瓶威士忌都能从容应对。",
            """
            ## 核心知识
            - 苏格兰、爱尔兰、美洲等产区的原料差异与典型风味
            - 熟成年份、橡木桶类型对口感的影响，以及哪些酒适合纯饮或加冰

            ## 品饮建议
            - 记录 Nose、Palate、Finish，建立个人威士忌档案
            - 分享靠谱的购酒/线下体验渠道，结交同好
            """
        );
    }
}

