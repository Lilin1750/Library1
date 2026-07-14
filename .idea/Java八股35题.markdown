# Java 知识复习 — 图书管理系统面试题集（35 题完整版）

> 所有题目均基于项目 [Library1](C:\Users\李霖\IdeaProjects\Library1) 的实际代码设计
>
> 涵盖：自定义异常 · 抽象类与接口 · 面向对象基础与设计 · Java 语法进阶

---

# 第一部分：自定义异常（基础篇 · 5 题）

---

## Q1（基础）RuntimeException vs Exception

**题目：**
你的 `BookException` 和 `UserException` 都继承了 `RuntimeException`，而不是 `Exception`。请问这两者有什么区别？为什么选择继承 `RuntimeException`？

**答案与讲解：**

Java 异常分两大类：

| 类别 | 父类 | 编译期检查 | 典型场景 |
|------|------|-----------|---------|
| **受检异常（Checked Exception）** | `Exception`（非 RuntimeException 子类） | 强制 try-catch 或 throws | IOException, SQLException |
| **非受检异常（Unchecked / Runtime Exception）** | `RuntimeException` | 不强制处理 | NullPointerException, 自定义业务异常 |

**项目中选择 RuntimeException 的原因：**

1. **不需要每处 try-catch**：如果继承 `Exception`，所有调 `findBookById()` 的地方都必须 try-catch 或 throws，代码臃肿。项目中 `MainClass` 在菜单循环外层统一 catch 父类 `BookException`，正是 Runtime 异常的典型用法——**在高层统一处理**。
2. **事务回滚语义**：企业框架（如 Spring）中，RuntimeException 默认触发事务回滚，Checked Exception 默认不回滚。

> ⚠️ 注意：不可恢复的场景用 RuntimeException，可恢复且调用方必须处理的场景更适合 Checked Exception。

---

## Q2（设计思路）多层异常层级的好处

**题目：**
你设计了 `BookException → BookNotFoundException / BookDuplicateException / InvalidPriceException` 的分层体系。这样有什么好处？

**答案与讲解：**

**分层设计的核心好处：**

1. **精准捕获 vs 统一处理两不误**
   - 精确场景：`catch (BookNotFoundException e)` → "请检查编号"
   - 统一场景：`catch (BookException e)` → 处理所有图书相关异常

2. **语义清晰**：异常类名直接表达错误原因

3. **扩展性强**：新增 `BookOutOfStockException extends BookException` 无需修改现有 catch 父类的代码

4. **类型安全**：方法可声明 `throws BookException` 而非宽泛的 `throws RuntimeException`

---

## Q3（语法细节）异常类型的语义匹配

**题目：**
在 `UserService.register()` 中，用户名为空时抛了 `UserDuplicateException("用户名不能为空！")`，这样合理吗？

**答案与讲解：**

**不合理，属于语义滥用。** 原因：

RuntimeException ├── BookException │ ├── BookNotFoundException │ ├── BookDuplicateException │ └── InvalidPriceException └── UserException ├── UserNotFoundException ├── UserDuplicateException └── PasswordException

- `UserDuplicateException` 表示"用户重复"，但实际错误是"用户名为空"
- 调用方 catch `UserDuplicateException` 后会误解为用户名已存在

**改进方案：**
java // 方案一：新增异常类 public class InvalidUsernameException extends UserException { public InvalidUsernameException(String message) { super(message); } }
// 方案二：使用 JDK 自带的 IllegalArgumentException if (username == null || username.trim().isEmpty()) { throw new IllegalArgumentException("用户名不能为空！"); }

> **核心原则：异常类命名必须准确反映错误语义。**

---

## Q4（深入理解）Multi-catch 语法

**题目：**

java catch (UserException | InputMismatchException e) { System.out.println("操作失败：" + e.getMessage()); }

请问这里使用了 Java 的什么特性？有什么优势和限制？能否分开写成两个 catch 块？

**答案与讲解：**

这是 **Java 7 引入的多异常捕获（Multi-catch）**。

**优势：**
- 代码简洁，避免重复的 catch 块
- `e` 默认隐式 `final`，不可重赋值，防止误操作

**限制：**
- `e` 的类型推断为这些异常的**共同父类中最具体的类型** → 此处为 `RuntimeException`
- 不能 catch 有父子关系的异常（如 `catch (Exception | RuntimeException e)` → 编译报错）

**能否分开写？**

java catch (UserException e) { ... } catch (InputMismatchException e) { ... }

可以。如果两个 catch 逻辑完全相同，**multi-catch 更优**（符合 DRY 原则）。

---

## Q5（实战）扩展异常类存储额外信息

**题目：**
假设要在 `InvalidPriceException` 里存非法的价格数值，应该怎么改？

**答案与讲解：**

java package exception;
public class InvalidPriceException extends BookException { private final double invalidPrice;
public InvalidPriceException(String message, double invalidPrice) {
super(message);
this.invalidPrice = invalidPrice;
}
public InvalidPriceException(String message) {
this(message, -1);  // 向后兼容
}

public double getInvalidPrice() {
return invalidPrice;
}
}

**调用示例：**
java catch (InvalidPriceException e) { if (e.getInvalidPrice() >= 0) { System.out.println("非法价格：" + e.getInvalidPrice()); } System.out.println(e.getMessage()); }

> **要点**：提供多构造器实现向后兼容，新增字段用 `final` 保证不可变。

---

# 第二部分：抽象类与接口（基础篇 · 4 题）

---

## Q6（基础）抽象类的特性

**题目：**
`User` 是抽象类，包含三个抽象方法。请问：
1. 抽象类能不能实例化？
2. 抽象类能不能没有抽象方法？
3. 子类不实现所有抽象方法会怎样？

**答案与讲解：**

**① 抽象类不能实例化：**
`new User(...)` 编译报错。因为抽象方法没有方法体，JVM 无法执行。

**② 抽象类可以没有抽象方法：**





