package net.tecnihardcore;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

/** Bounded meshes; the funnel and dangerous silhouettes survive minimal particle settings. */
final class StormGeometry {
    static void clouds(MatrixStack m,VertexConsumer v,Vec3d p,double time,float opacity){for(int i=0;i<7;i++){double a=i*Math.PI*2/7+time*.001;sphere(m,v,p.add(Math.cos(a)*34,58+Math.sin(a*2)*3,Math.sin(a)*34),34,8,27,16,5,opacity*.7F,.28F,.3F,.34F,i*.4);}}
    static void quake(MatrixStack m,VertexConsumer v,VertexConsumer lines,Vec3d center,double pulse,float opacity){if(pulse<40||pulse>90)return;float fade=(float)(90-pulse)/50;for(int arm=0;arm<8;arm++){double a=arm*Math.PI/4;Vec3d last=center.add(0,.03,0);for(int step=1;step<=7;step++){double d=step*6,wiggle=Math.sin(arm*3+step*2.2)*1.7;Vec3d next=center.add(Math.cos(a)*d+Math.sin(a)*wiggle,.045,Math.sin(a)*d-Math.cos(a)*wiggle);beam(m,lines,last,next,.1,.92F,.57F,.18F,opacity*fade);if(step%2==0)sphere(m,v,next.add(0,.25+Math.sin(pulse*.1+step)*.2,0),.6,.18,.45,6,3,opacity*fade*.7F,.48F,.4F,.31F,a);last=next;}}}
    static void tornado(MatrixStack m,VertexConsumer v,VertexConsumer lines,Vec3d p,double time,float opacity,boolean distant){
        int segments=distant?24:40,bands=distant?16:24;
        for(int layer=0;layer<(distant?2:3);layer++)for(int y=0;y<bands;y++){
            double f=y/(double)bands,g=(y+1)/(double)bands;
            double r0=ExpansionRules.tornadoRadius(f)*(1-layer*.11),r1=ExpansionRules.tornadoRadius(g)*(1-layer*.11);
            for(int i=0;i<segments;i++){
                double a=i*Math.PI*2/segments+time*.018+f*5+layer,b=(i+1)*Math.PI*2/segments+time*.018+f*5+layer;
                quad(m,v,p.add(Math.cos(a)*r0,f*200,Math.sin(a)*r0),p.add(Math.cos(b)*r0,f*200,Math.sin(b)*r0),p.add(Math.cos(b+.2)*r1,g*200,Math.sin(b+.2)*r1),p.add(Math.cos(a+.2)*r1,g*200,Math.sin(a+.2)*r1),i/(float)segments,(i+1)/(float)segments,(float)(f*4+time*.002),(float)(g*4+time*.002),opacity*(layer==0?.84F:.48F),.64F,.67F,.7F);
            }
        }
        for(int i=0;i<(distant?4:7);i++){double a=i*Math.PI*2/7+time*.003;sphere(m,v,p.add(Math.cos(a)*29,185+Math.sin(a*2)*2,Math.sin(a)*29),29,12,29,distant?12:20,5,opacity*.68F,.42F,.45F,.5F,i*.3);}
        // Dust skirt and floating rubble are cosmetic geometry, never real blocks.
        sphere(m,v,p.add(0,1,0),27,1,27,20,3,opacity*.3F,.6F,.49F,.3F,time*.004);
        for(int i=0;i<(distant?5:16);i++){double a=time*.033+i*2.4,y=9+(i*5+time*.16)%80,r=7+y*.14;sphere(m,v,p.add(Math.cos(a)*r,y,Math.sin(a)*r),.45+i%3*.25,.5,.5,6,3,opacity,.5F,.45F,.4F,i);}
        if((int)time%80<7){Vec3d origin=p.add(0,30+((int)time/80%3)*24,0);lightning(m,lines,origin,35,((int)time/80)*17,opacity*.8F);}
    }

    static void meteor(MatrixStack m,VertexConsumer v,VertexConsumer lines,Vec3d target,double cycle,float opacity){
        if(cycle<=60){double remaining=Math.max(0,60-cycle);Vec3d p=target.add(remaining*.22,remaining*.8,remaining*.12);
            sphere(m,v,p,1.8,1.6,1.8,14,8,opacity,1,.75F,.4F,cycle*.08);
            for(int i=0;i<7;i++)sphere(m,v,p.add(i*.6,i*2.1,i*.32),Math.max(.15,1.5-i*.18),2,Math.max(.15,1.5-i*.18),8,3,opacity*(.55F-i*.065F),1,.28F+i*.05F,.05F,cycle*.1+i);
        }else if(cycle<78){float fade=(float)(78-cycle)/18;sphere(m,v,target.add(0,.08,0),4,.08,4,20,3,opacity*fade*.6F,.34F,.16F,.08F,0);for(int i=0;i<3;i++)ring(m,lines,target.add(0,.12,0),4+(cycle-60)*.25+i*.35,1,.55F,.16F,fade);}
    }

    static void lightning(MatrixStack m,VertexConsumer lines,Vec3d target,double height,long seed,float opacity){
        var random=new java.util.Random(seed);Vec3d previous=target.add(0,height,0);
        for(int i=1;i<=9;i++){Vec3d next=target.add(i==9?0:random.nextDouble()*5-2.5,height*(1-i/9.0),i==9?0:random.nextDouble()*5-2.5);beam(m,lines,previous,next,.09,.84F,.9F,1,opacity);if(i==3||i==5){Vec3d branch=next.add(random.nextBoolean()?7:-7,-height*.18,random.nextDouble()*6-3);beam(m,lines,next,branch,.045,.64F,.75F,1,opacity*.7F);}previous=next;}
    }
    static void beam(MatrixStack m,VertexConsumer lines,Vec3d a,Vec3d b,double width,float r,float g,float blue,float alpha){Vec3d n=b.subtract(a).normalize();for(double offset:new double[]{-width,0,width}){lines.vertex(m.peek().getPositionMatrix(),(float)(a.x+offset),(float)a.y,(float)a.z).color(r,g,blue,alpha).normal(m.peek().getNormalMatrix(),(float)n.x,(float)n.y,(float)n.z).next();lines.vertex(m.peek().getPositionMatrix(),(float)(b.x+offset),(float)b.y,(float)b.z).color(r,g,blue,alpha).normal(m.peek().getNormalMatrix(),(float)n.x,(float)n.y,(float)n.z).next();}}
    static void ring(MatrixStack m,VertexConsumer lines,Vec3d p,double radius,float r,float g,float b,float a){for(int i=0;i<48;i++){double angle=i*Math.PI/24,next=(i+1)*Math.PI/24;beam(m,lines,p.add(Math.cos(angle)*radius,.15,Math.sin(angle)*radius),p.add(Math.cos(next)*radius,.15,Math.sin(next)*radius),.055,r,g,b,a);}}
    private static void sphere(MatrixStack m,VertexConsumer v,Vec3d p,double rx,double ry,double rz,int segments,int bands,float alpha,float r,float g,float b,double rotation){for(int j=0;j<bands;j++){double a=-Math.PI/2+j*Math.PI/bands,next=a+Math.PI/bands;for(int i=0;i<segments;i++){double u=i*Math.PI*2/segments+rotation,w=u+Math.PI*2/segments;quad(m,v,point(p,rx,ry,rz,a,u),point(p,rx,ry,rz,a,w),point(p,rx,ry,rz,next,w),point(p,rx,ry,rz,next,u),i/(float)segments,(i+1)/(float)segments,j/(float)bands,(j+1)/(float)bands,alpha,r,g,b);}}}
    private static Vec3d point(Vec3d p,double x,double y,double z,double a,double u){double n=1+.045*Math.sin(u*5+a*8);return p.add(Math.cos(a)*Math.cos(u)*x*n,Math.sin(a)*y,Math.cos(a)*Math.sin(u)*z*n);}
    private static void quad(MatrixStack m,VertexConsumer v,Vec3d a,Vec3d b,Vec3d c,Vec3d d,float u,float u1,float y,float y1,float alpha,float r,float g,float blue){vertex(m,v,a,u,y,alpha,r,g,blue);vertex(m,v,b,u1,y,alpha,r,g,blue);vertex(m,v,c,u1,y1,alpha,r,g,blue);vertex(m,v,d,u,y1,alpha,r,g,blue);}
    private static void vertex(MatrixStack m,VertexConsumer v,Vec3d p,float u,float y,float a,float r,float g,float b){v.vertex(m.peek().getPositionMatrix(),(float)p.x,(float)p.y,(float)p.z).color(r,g,b,a).texture(u,y).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(m.peek().getNormalMatrix(),0,1,0).next();}
}
