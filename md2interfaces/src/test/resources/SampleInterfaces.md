# Service Offerings and Sessions

Services and their session configurations

| Term    | Value                                       |
|---------|---------------------------------------------|
| subject | Service offerings and sessions example file |
| date    | 2019-08-07T09:30:00Z                        |

## Interface Private

Order entry service with algorithm control

#### Protocols

| Layer     | Name       | Orchestration                                 | Reliability | Messagecast |
|-----------|------------|-----------------------------------------------|-------------|-------------|
| Service   | orderEntry | https://mydomain.com/orchestra/orderEntry.xml |             |             |
| UI        | ATDL       | https://mydomain.com/orchestra/algo.xml       |             |             |
| Encoding  | TagValue   |                                               |             |             |
| Session   | FIXT.1.1   | https://mydomain.com/orchestra/session.xml    | RECOVERABLE |             |
| Transport | TCP        |                                               |             | UNICAST     |

### Session XYZ-ABC

#### Identifiers

| Name         | Value |
|--------------|-------|
| SenderCompID | XYZ   |
| TargetCompID | ABC   |

#### Protocols

| Layer     | Use       | Address       | Messagecast |
|-----------|-----------|---------------|-------------|
| Transport | primary   | 10.96.1.2:567 | UNICAST     |
| Transport | secondary | 10.96.2.2:567 | UNICAST     |

## Interface Public

Market data multicast

#### Protocols

| Layer     | Name         | Orchestration                                 | Address   | Messagecast | Reliability |
|-----------|--------------|-----------------------------------------------|-----------|-------------|-------------|
| Service   | marketData   | https://mydomain.com/orchestra/marketData.xml |           |             |             |
| Encoding  | SBE          |                                               |           |             |             |
| Transport | UDPMulticast |                                               | 224.0.0.1 | MULTICAST   | BEST_EFFORT |

