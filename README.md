# ToDoAgent
Microsoft Azure Enabled LLM Agent that helps you read apps notifications, emails, etc. Then generate a ToDo list with key person and deadline. The future version may directly help you finish the things on the generated ToDoList



## SQL Setup

### Prerequisites

- Docker installed on your machine. You can download and install Docker from [Docker's official website](https://www.docker.com/products/docker-desktop).

### Run Mysql in docker

```bash
docker run --name mysql-container -p 3306:3306 -e MYSQL_ROOT_PASSWORD=your_password -d mysql:latest
```

### 4. Access the MySQL Shell

To access the MySQL shell inside the container, run:

```bash
docker exec -it mysql-container mysql -uroot -p
```

Enter the root password when prompted.

