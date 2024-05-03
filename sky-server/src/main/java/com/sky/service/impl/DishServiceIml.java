package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j

public class DishServiceIml implements DishService {

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增菜品
     *
     * @param dishDTO
     * @return
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //向菜品表插入数据

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);


        dishMapper.insert(dish);
        long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();

        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });

            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);


        return new PageResult(page.getTotal(), page.getResult());


    }

    /**
     * 菜品批量删除
     *
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {

        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }

        }
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);

        }

/*        for (Long id : ids) {
            dishMapper.deleteById(id);
            dishFlavorMapper.deleteByDishId(id);

        }*/

        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);


    }

    @Override
    public DishVO getByIdWithFlavor(long id) {

        DishVO dishVO = new DishVO();
        Dish dish= dishMapper.getById(id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.getById(id);
        BeanUtils.copyProperties(dish ,dishVO );
        dishVO.setFlavors(dishFlavors);


        return dishVO;
    }

    @Override
    public void updateWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);

        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });

            dishFlavorMapper.insertBatch(flavors);
        }


    }
}
