# Show Bidders

**URL** : `/bidders/`

**Method** : `GET`

**Auth required** : NO

**Permissions required** : None

**Data constraints** : `{}`

## Success Responses

**Condition** : There is no Bidder in the system yet.

**Code** : `200 OK`

**Content** : `{[]}`

### OR

**Condition** : There is at least one Bidder in the system.

**Code** : `200 OK`

**Content** : In this example, the User can see two Bidders in that order:

```json
[
    {
        "bidders": [
            {
                "bidderId": "1"
            },
            {
                "bidderId": "2"
            }
        ]
    }
]
```
