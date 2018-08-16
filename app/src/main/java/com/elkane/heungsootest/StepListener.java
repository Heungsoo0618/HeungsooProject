/*
 * ***************************************************************************************
 *  ** Hyundai Motor India Engineering Pvt Ltd
 *  ** Electronics Engineering Dept-1., - Software Development Team
 *  ** Do Not Copy Without Prior Permission
 *  *****************************************************************************************
 *  ** Project Name: ESDS Development
 *  ** Target: Proof Of Concept (NU Head Unit)
 *  ** File Name: StepListener.java
 *  ** @Author: Sai Sriram Madhiraju
 *  ** @Co-Author: Sivaram Boina
 *  ** Completion Date: 29-12-2017
 *  ***************************************************************************************
 */

package com.elkane.heungsootest;

/**
 * Interface for handling the time taked by each second step
 */
public interface StepListener {
    public void step(long timeNs);
}
