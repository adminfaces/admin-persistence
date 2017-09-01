package com.github.adminfaces.persistence;

import com.github.adminfaces.persistence.model.*;
import com.github.adminfaces.persistence.service.CarService;
import com.github.adminfaces.persistence.service.CrudService;
import com.github.adminfaces.persistence.service.Service;
import com.github.database.rider.cdi.api.DBUnitInterceptor;
import com.github.database.rider.core.api.dataset.DataSet;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.*;

@RunWith(CdiTestRunner.class)
@DBUnitInterceptor
@DataSet(cleanBefore = true)
public class CrudServiceIt {

    @Inject
    CarService carService;

    @Inject
    @Service
    CrudService<Car, Integer> crudService;

    @Inject
    @Service
    CrudService<SalesPoint, SalesPointPK> salesPointService;


    @Test
    @DataSet("cars.yml")
    public void shouldCountCars() {
        assertEquals(carService.count(), 4);
    }

    @Test
    @DataSet("cars.yml")
    public void shouldFindCarById() {
        Car car = carService.findById(1);
        assertThat(car).isNotNull()
                .extracting("id")
                .contains(new Integer(1));
    }

    @Test
    @DataSet("cars.yml")
    public void shouldFindCarByExample() {
        Car carExample = new Car().model("Ferrari");
        List<Car> cars = carService.example(carExample, Car_.model).getResultList();
        assertThat(cars).isNotNull()
                .hasSize(1)
                .extracting("id")
                .contains(new Integer(1));
    }

    @Test
    public void shouldNotInsertCarWithoutName() {
        long countBefore = carService.count();
        assertEquals(countBefore, 0);
        Car newCar = new Car().model("My Car").price(1d);
        try {
            carService.insert(newCar);
        } catch (RuntimeException e) {
            assertEquals("Car name cannot be empty", e.getMessage());
        }
    }

    @Test
    public void shouldNotInsertCarWithoutModel() {
        Car newCar = new Car().name("My Car")
                .price(1d);
        try {
            carService.insert(newCar);
        } catch (RuntimeException e) {
            assertEquals("Car model cannot be empty", e.getMessage());
        }
    }

    @Test
    @DataSet("cars.yml")
    public void shouldNotInsertCarWithDuplicateName() {
        Car newCar = new Car().model("My Car")
                .name("ferrari spider")
                .price(1d);
        try {
            carService.insert(newCar);
        } catch (RuntimeException e) {
            assertEquals("Car name must be unique", e.getMessage());
        }
    }

    @Test
    public void shouldInsertCar() {
        long countBefore = carService.count();
        assertEquals(countBefore, 0);
        Car newCar = new Car().model("My Car")
                .name("car name").price(1d);
        carService.insert(newCar);
        assertEquals(countBefore + 1, carService.count());
    }

    @Test
    @DataSet("cars.yml")
    public void shouldRemoveCar() {
        assertEquals(carService.count(carService.criteria().eq(Car_.id, 1)), 1);
        Car car = carService.findById(1);
        assertNotNull(car);
        carService.remove(car);
        assertEquals(carService.count(carService.criteria().eq(Car_.id, 1)), 0);
    }

    @Test
    @DataSet("cars.yml")
    public void shouldListCarsModel() {
        List<Car> cars = carService.listByModel("%porche%");
        assertThat(cars).isNotNull().hasSize(2);
    }

    @Test
    @DataSet("cars.yml")
    public void shouldPaginateCars() {
        Filter<Car> carFilter = new Filter<Car>().setFirst(0).setPageSize(1);
        List<Car> cars = carService.paginate(carFilter);
        assertNotNull(cars);
        assertEquals(cars.size(), 1);
        assertEquals(cars.get(0).getId(), new Integer(1));
        carFilter.setFirst(1);//get second database page
        cars = carService.paginate(carFilter);
        assertNotNull(cars);
        assertEquals(cars.size(), 1);
        assertEquals(cars.get(0).getId(), new Integer(2));
        carFilter.setFirst(0);
        carFilter.setPageSize(4);
        cars = carService.paginate(carFilter);
        assertEquals(cars.size(), 4);
    }

    @Test
    @DataSet("cars.yml")
    public void shouldPaginateAndSortCars() {
        Filter<Car> carFilter = new Filter<Car>()
                .setFirst(0)
                .setPageSize(4)
                .setSortField("model")
                .setAdminSort(AdminSort.DESCENDING);
        List<Car> cars = carService.paginate(carFilter);
        assertThat(cars).isNotNull().hasSize(4);
        assertTrue(cars.get(0).getModel().equals("Porche274"));
        assertTrue(cars.get(3).getModel().equals("Ferrari"));
    }

    @Test
    @DataSet("cars.yml")
    public void shouldPaginateCarsByModel() {
        Car carExample = new Car().model("Ferrari");
        Filter<Car> carFilter = new Filter<Car>().
                setFirst(0).setPageSize(4)
                .setEntity(carExample);
        List<Car> cars = carService.paginate(carFilter);
        assertThat(cars).isNotNull().hasSize(1)
                .extracting("model").contains("Ferrari");
    }

    @Test
    @DataSet("cars.yml")
    public void shouldPaginateCarsByPrice() {
        Car carExample = new Car().price(12999.0);
        Filter<Car> carFilter = new Filter<Car>().setFirst(0).setPageSize(2).setEntity(carExample);
        List<Car> cars = carService.paginate(carFilter);
        assertThat(cars).isNotNull().hasSize(1)
                .extracting("model").contains("Mustang");
    }

    @Test
    @DataSet("cars.yml")
    public void shouldPaginateCarsByIdInParam() {
        Filter<Car> carFilter = new Filter<Car>().setFirst(0).setPageSize(2).addParam("id", 1);
        List<Car> cars = carService.paginate(carFilter);
        assertThat(cars).isNotNull().hasSize(1)
                .extracting("id").contains(1);
    }

    @Test
    @DataSet("cars.yml")
    public void shouldListCarsByPrice() {
        List<Car> cars = carService.criteria()
                .between(Car_.price, 1000D, 2450.9D)
                .orderAsc(Car_.price).getResultList();
        //ferrari and porche
        assertThat(cars).isNotNull()
                .hasSize(2).extracting("model")
                .contains("Porche", "Ferrari");
    }

    @Test
    @DataSet("cars.yml")
    public void shouldGetCarModels() {
        List<String> models = carService.getModels("po");
        //porche and Porche274
        assertThat(models).isNotNull().hasSize(2)
                .contains("Porche", "Porche274");
    }

    @Test
    @DataSet("cars.yml")
    public void shoulListCarsUsingCrudUtility() {
        assertEquals(4, crudService.count());
        long count = crudService.count(crudService.criteria()
                .likeIgnoreCase(Car_.model, "%porche%")
                .gtOrEq(Car_.price, 10000D));
        assertEquals(1, count);

    }

    @Test
    @DataSet("cars.yml")
    public void shouldFindCarsByExample() {
        Car carExample = new Car().model("Ferrari");
        List<Car> cars = crudService.example(carExample, Car_.model).getResultList();
        assertThat(cars).isNotNull().hasSize(1)
                .extracting("model")
                .contains("Ferrari");

        carExample = new Car().model("porche").name("%avenger");
        cars = crudService.exampleLike(carExample, Car_.name, Car_.model).getResultList();

        assertThat(cars).isNotNull().hasSize(1)
                .extracting("name")
                .contains("porche avenger");

        cars = crudService.exampleLike(carExample).getResultList();

        //should list all when no property is provided
        assertThat(cars).isNotNull().hasSize(4);

    }

    @Test
    @DataSet("cars.yml")
    public void shoulGetTotalPriceByModel() {
        assertEquals((Double) 20380.53, carService.getTotalPriceByModel(new Car().model("%porche%")));
    }

    @Test
    @DataSet("cars-full.yml")
    public void shouldFindByCompositeKey() {
        SalesPointPK pk = new SalesPointPK(1L,3L);
        SalesPoint salesPoint = salesPointService.findById(pk);
        assertThat(salesPoint).isNotNull().extracting("name")
                .contains("Ford Motors2");
    }

     @Test
    @DataSet("cars-full.yml")
    public void shouldListCarsBySalesPoint() {
        List<Car> carsFound = carService.findBySalesPoint(new SalesPoint(new SalesPointPK(2L, 1L)));
        assertThat(carsFound).isNotNull().hasSize(1)
                .extracting("name")
                .contains("Sentra");
    }

    @Test
    @DataSet("cars-full.yml")
    public void shouldListCarsBySalesPointAddress() {
        List<Car> carsFound = carService.findBySalesPointAddress("Nissan address");
        assertThat(carsFound).isNotNull().hasSize(1)
                .extracting("name")
                .contains("Sentra");
    }


    @Test
    @DataSet("cars-full.yml")
    public void shouldListCarsBySalesPointAddressByExample() {
        Car carExample = new Car();
        SalesPoint salesPoint = new SalesPoint(new SalesPointPK(2L, 1L));
        List<SalesPoint> salesPoints = new ArrayList<>();
        salesPoints.add(salesPoint);
        carExample.setSalesPoints(salesPoints);
        List<Car> carsFound = carService.example(carExample,Car_.salesPoints).getResultList();
           assertThat(carsFound).isNotNull().hasSize(1)
                .extracting("name")
                .contains("Sentra");
    }



}
