```mermaid
graph TD
    root["root (Ktor)"]-->app
    app-->interfaces
    di-->datasource
    app-->di
    di-->interfaces
    datasource-->interfaces---datasource
    app-->base
    datasource-->base
    subgraph app 
        app
        interfaces
    end
```