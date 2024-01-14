import React from "react";
import {Card, CardContent, Typography} from "@mui/material";

interface PropertyCardProps {
    title: string;
    value: string;
}

const PropertyCard: React.FC<PropertyCardProps> = ({ title, value }) => {
    return (
        <Card variant="outlined">
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    {title}
                </Typography>
                <Typography variant="h5" component="h2">
                    {value}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default PropertyCard;
