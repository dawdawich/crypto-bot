import React, {RefObject, useEffect, useState} from 'react';

type DraggableProps = {
    children: React.ReactNode;
    reference: RefObject<HTMLDivElement>;
}

const Draggable: React.FC<DraggableProps> = ({ children, reference }) => {
    const [isDragging, setIsDragging] = useState(false);
    const [startY, setStartY] = useState(0);
    const [scrollStartY, setScrollStartY] = useState(0);

    const handleMouseDown = (event: React.MouseEvent<HTMLDivElement>) => {
        setIsDragging(true);
        setStartY(event.clientY);
        if (reference.current) {
            setScrollStartY(reference.current.scrollTop);
        }
    };


    useEffect(() => {

        const handleMouseMove = (event: MouseEvent) => {
            if (isDragging && reference.current) {
                const currentY = event.clientY;
                const deltaY = currentY - startY;
                reference.current.scrollTop = scrollStartY - deltaY;
            }
        };

        const handleMouseUp = () => {
            setIsDragging(false);
        };

        const handleMouseLeave = () => setIsDragging(false);

        if (isDragging) {
            document.addEventListener('mousemove', handleMouseMove);
            document.addEventListener('mouseup', handleMouseUp);
            document.addEventListener('mouseleave', handleMouseLeave);
        }

        return () => {
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
            document.removeEventListener('mouseleave', handleMouseLeave);
        };
    }, [isDragging, startY, reference, scrollStartY]); // Only re-run the effect if dragging state or startY changes

    return (
        <div
            className='disable-scrollbar'
            ref={reference}
            onMouseDown={handleMouseDown}
            style={{
                cursor: isDragging ? 'grabbing' : 'grab',
                userSelect: 'none',
                overflowY: 'hidden',
                height: '400px'
            }}
        >
            {children}
        </div>
    );
};

export default Draggable;
