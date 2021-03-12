package com.kuang.controller;

import com.kuang.dao.DepartmentDao;
import com.kuang.dao.EmployeeDao;
import com.kuang.pojo.Department;
import com.kuang.pojo.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;

@Controller
public class EmployeeController {
    @Autowired
    EmployeeDao employeeDao;

    @Autowired
    DepartmentDao departmentDao;

    @RequestMapping("/emps")
    public String list(Model model) {
        Collection<Employee> employees = employeeDao.getAll();
        model.addAttribute("emps", employees);
        return "emp/list";
    }

    @GetMapping("/emp") // get方式获取请求
    public String toAddpage(Model model) {
        //查出所有部门的信息
        Collection<Department> departments = departmentDao.getDepartments();
        model.addAttribute("departments", departments);
        return "/emp/add";
    }

    @PostMapping("/emp") // post方式获取请求
    public String addEmp(Employee employee) {
        //添加操作 forward
        employeeDao.save(employee); //调用底层业务方法保存员工信息
        return "redirect:/emps";
    }

    //去员工修改页面
    @GetMapping("/emp/{id}")
    public String toUpdateEmp(@PathVariable("id") Integer id, Model model) {
        //查出原来的数据
        Employee employee = employeeDao.getEmployeeById(id);
        model.addAttribute("emp", employee);
        //查出所有部门的信息
        Collection<Department> departments = departmentDao.getDepartments();
        model.addAttribute("departments", departments);
        return "emp/update";
    }

    @PostMapping("/updateEmp")
    public String updateEmp(Employee employee) {
        employeeDao.save(employee);
        return "redirect:/emps";
    }

    //删除员工
    @GetMapping("/deleteemp/{id}")
    public String deleteEmp(@PathVariable("id")int id) {
        employeeDao.delete(id);
        return "redirect:/emps";
    }


}
