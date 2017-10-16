package com.databazoo.components.containers;

import java.awt.*;

import javax.swing.*;

import org.junit.Test;

import junit.framework.Assert;

public class ContainerTest {

    @Test
    public void horizontalContainer() throws Exception {
        JPanel container;
        Component lab1 = new JLabel("1");
        Component lab2 = new JLabel("2");
        Component lab3 = new JLabel("3");
        Dimension size = new Dimension(100, 200);

        container = new HorizontalContainer();
        Assert.assertTrue(container.getLayout() instanceof BorderLayout);

        container = new HorizontalContainer(lab1, lab2, lab3);
        Assert.assertTrue(container.getLayout() instanceof BorderLayout);
        Assert.assertEquals(lab1, container.getComponent(0));
        Assert.assertEquals(lab2, container.getComponent(1));
        Assert.assertEquals(lab3, container.getComponent(2));
        Assert.assertTrue(container.getMinimumSize().getHeight() < size.getHeight() && container.getMinimumSize().getWidth() < size.getWidth());
        Assert.assertTrue(container.getMaximumSize().getHeight() > size.getHeight() && container.getMaximumSize().getWidth() > size.getWidth());

        container = new HorizontalContainer.Builder().max(size).left(lab1).center(lab2).right(lab3).build();
        Assert.assertTrue(container.getLayout() instanceof BorderLayout);
        Assert.assertEquals(lab1, container.getComponent(0));
        Assert.assertEquals(lab2, container.getComponent(1));
        Assert.assertEquals(lab3, container.getComponent(2));
        Assert.assertTrue(container.getMinimumSize().getHeight() < size.getHeight() && container.getMinimumSize().getWidth() < size.getWidth());
        Assert.assertTrue(container.getMaximumSize().getHeight() == size.getHeight() && container.getMaximumSize().getWidth() == size.getWidth());

        container = new HorizontalContainer.Builder().min(size).left(lab1).center(lab2).right(lab3).build();
        Assert.assertTrue(container.getLayout() instanceof BorderLayout);
        Assert.assertEquals(lab1, container.getComponent(0));
        Assert.assertEquals(lab2, container.getComponent(1));
        Assert.assertEquals(lab3, container.getComponent(2));
        Assert.assertTrue(container.getMinimumSize().getHeight() == size.getHeight() && container.getMinimumSize().getWidth() == size.getWidth());
        Assert.assertTrue(container.getMaximumSize().getHeight() > size.getHeight() && container.getMaximumSize().getWidth() > size.getWidth());
    }

    @Test
    public void verticalContainer() throws Exception {
        JPanel container;
        Component lab1 = new JLabel("1");
        Component lab2 = new JLabel("2");
        Component lab3 = new JLabel("3");
        Dimension size = new Dimension(100, 200);

        container = new VerticalContainer();
        Assert.assertTrue(container.getLayout() instanceof BorderLayout);

        container = new VerticalContainer(lab1, lab2, lab3);
        Assert.assertTrue(container.getLayout() instanceof BorderLayout);
        Assert.assertEquals(lab1, container.getComponent(0));
        Assert.assertEquals(lab2, container.getComponent(1));
        Assert.assertEquals(lab3, container.getComponent(2));
        Assert.assertTrue(container.getMinimumSize().getHeight() < size.getHeight() && container.getMinimumSize().getWidth() < size.getWidth());
        Assert.assertTrue(container.getMaximumSize().getHeight() > size.getHeight() && container.getMaximumSize().getWidth() > size.getWidth());

        container = new VerticalContainer.Builder().max(size).top(lab1).center(lab2).bottom(lab3).build();
        Assert.assertTrue(container.getLayout() instanceof BorderLayout);
        Assert.assertEquals(lab1, container.getComponent(0));
        Assert.assertEquals(lab2, container.getComponent(1));
        Assert.assertEquals(lab3, container.getComponent(2));
        Assert.assertTrue(container.getMinimumSize().getHeight() < size.getHeight() && container.getMinimumSize().getWidth() < size.getWidth());
        Assert.assertTrue(container.getMaximumSize().getHeight() == size.getHeight() && container.getMaximumSize().getWidth() == size.getWidth());

        container = new VerticalContainer.Builder().min(size).top(lab1).center(lab2).bottom(lab3).build();
        Assert.assertTrue(container.getLayout() instanceof BorderLayout);
        Assert.assertEquals(lab1, container.getComponent(0));
        Assert.assertEquals(lab2, container.getComponent(1));
        Assert.assertEquals(lab3, container.getComponent(2));
        Assert.assertTrue(container.getMinimumSize().getHeight() == size.getHeight() && container.getMinimumSize().getWidth() == size.getWidth());
        Assert.assertTrue(container.getMaximumSize().getHeight() > size.getHeight() && container.getMaximumSize().getWidth() > size.getWidth());
    }

}