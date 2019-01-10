package org.intermine.api.bag.operations;

import java.util.HashSet;
import java.util.Set;

import org.intermine.model.InterMineId;
import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.InterMineBag;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Employable;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Secretary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.DynamicUtil;

public abstract class AbstractBagOperationTestCase extends InterMineAPITestCase {

    Set<InterMineId> employees1;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        insertData();
        setUpBags();
    }

    Set<InterMineId> employees2;
    Set<InterMineId> managers;
    Set<InterMineId> ceos;
    Set<InterMineId> contractors;
    Set<InterMineId> employables;
    Set<InterMineId> secretaries;
    protected InterMineBag bagA;
    protected InterMineBag bagB;
    protected InterMineBag bagC;
    protected InterMineBag bagD;
    protected InterMineBag bagE;
    protected InterMineBag bagF;
    protected InterMineBag bagG;

    public AbstractBagOperationTestCase(String arg) {
        super(arg);
    }

    private void insertData() throws Exception {
        Long start = System.currentTimeMillis();
        ObjectStoreWriter osw = os.getNewWriter();
    
        employees1 = new HashSet<InterMineId>();
        employees2 = new HashSet<InterMineId>();
        employables = new HashSet<InterMineId>();
        managers = new HashSet<InterMineId>();
        ceos = new HashSet<InterMineId>();
        secretaries = new HashSet<InterMineId>();
        for (int i = 0; i < 10; i++) {
            Employee emp = new Employee();
            emp.setName("Employee" + Character.toString((char) (i + 65)));
            emp.setAge(i - 40);
            osw.store(emp);
            InterMineId id = emp.getId();
            Set<InterMineId> set = (i % 2 == 0) ? employees1 : employees2;
            set.add(id); // 5 to each.
            if (i % 3 == 0) employables.add(id); // 4
            // + 2, So that there is an intersection between emps1 & emps2
            if (i % 7 == 0) employees1.add(id); // 2, one of which is shared.
        }
        for (int i = 0; i < 10; i++) {
            Employee emp = new Manager();
            emp.setName("Manager" + Character.toString((char) (i + 65)));
            emp.setAge(i - 40);
            osw.store(emp);
            managers.add(emp.getId()); // 10
            if (i % 2 == 0) employees1.add(emp.getId()); // 5
            if (i % 3 == 0) employables.add(emp.getId()); // 4
        }
        for (int i = 0; i < 10; i++) {
            Employee emp = new CEO();
            emp.setName("CEO" + Character.toString((char) (i + 65)));
            emp.setAge(i - 40);
            osw.store(emp);
            ceos.add(emp.getId()); // 10
            if (i % 2 == 0) managers.add(emp.getId()); // 5
            if (i % 3 == 0) employees1.add(emp.getId()); // 4
            if (i % 4 == 0) employables.add(emp.getId()); // 3
        }
        contractors = new HashSet<InterMineId>();
        for (int i = 65; i < 75; i++) {
            Contractor emp = new Contractor();
            emp.setName("Contractor" + Character.toString((char) i));
            osw.store(emp);
            contractors.add(emp.getId()); // 10
            if (i % 2 == 0) employables.add(emp.getId()); // 5
        }
        for (int i = 65; i < 75; i++) {
            Employable x = DynamicUtil.simpleCreateObject(Employable.class);
            x.setName("Employable" + Character.toString((char) i));
            osw.store(x);
            employables.add(x.getId()); // 10
        }
        //types = new HashSet(Arrays.asList(Company.class));
        for (int i = 65; i < 70; i++) {
            Secretary s = new Secretary();
            s.setName("Secretary" + Character.toString((char) i));
            osw.store(s);
            secretaries.add(s.getId());
        }
    
        osw.close();
    }

    private void setUpBags() throws Exception {
        bagA = testUser.createBag("emp1", "Employee", "bag of emps", im.getClassKeys());
        bagB = testUser.createBag("emp2", "Employee", "bag of emps", im.getClassKeys());
        bagC = testUser.createBag("mans", "Manager", "bag of mans", im.getClassKeys());
        bagD = testUser.createBag("ceos", "CEO", "bag of ceos", im.getClassKeys());
        bagE = testUser.createBag("cons", "Contractor", "bag of things", im.getClassKeys());
        bagF = testUser.createBag("empabls", "Employable", "bag of things", im.getClassKeys());
        bagG = testUser.createBag("secretaries", "Secretary", "bag of secretaries", im.getClassKeys());
    
        bagA.addIdsToBag(employees1, "Employee");
        bagB.addIdsToBag(employees2, "Employee");
        bagC.addIdsToBag(managers, "Manager");
        bagD.addIdsToBag(ceos, "CEO");
        bagE.addIdsToBag(contractors, "Contractor");
        bagF.addIdsToBag(employables, "Employable");
        bagG.addIdsToBag(secretaries, "Secretary");
    }

}